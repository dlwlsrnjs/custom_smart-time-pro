from pathlib import Path
import shutil, re, json, xml.etree.ElementTree as ET
import argparse
parser=argparse.ArgumentParser()
parser.add_argument('--workspace',required=True)
parser.add_argument('--output-dir',required=True)
args=parser.parse_args()
original=Path(args.workspace).resolve()
target=Path(args.output_dir).resolve()
root=target.parent
if target.exists():raise SystemExit('Use fresh output path')
shutil.copytree(original/'diz-custom',target,ignore=shutil.ignore_patterns('build'))
a='{http://schemas.android.com/apk/res/android}';ET.register_namespace('android',a[1:-1])
tree=ET.parse(target/'AndroidManifest.xml');m=tree.getroot();app=m.find('application')
m.set(a+'versionCode','10');m.set(a+'versionName','0.4.0')
keep={'BLUETOOTH','BLUETOOTH_ADMIN','BLUETOOTH_SCAN','BLUETOOTH_CONNECT','ACCESS_FINE_LOCATION','ACCESS_COARSE_LOCATION','ACTIVITY_RECOGNITION','POST_NOTIFICATIONS','FOREGROUND_SERVICE','FOREGROUND_SERVICE_CONNECTED_DEVICE','VIBRATE','WAKE_LOCK'}
removed=[];seen=set()
for p in list(m):
 if p.tag.startswith('uses-permission'):
  name=p.get(a+'name','');short=name.rsplit('.',1)[-1]
  if name.startswith('android.permission.') and short in keep and name not in seen:seen.add(name)
  elif name=='com.diz.sw01.custom.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION':pass
  else:removed.append(name);m.remove(p)
for name in ('FOREGROUND_SERVICE_LOCATION',):ET.SubElement(m,'uses-permission',{a+'name':'android.permission.'+name})
ad_prefix=('com.google.android.gms.ads','com.facebook.ads','com.applovin','com.ironsource','com.unity3d','com.bytedance.sdk.openadsdk','com.vungle','com.mbridge','com.chartboost','com.amazon.device.ads','com.inmobi')
components=[]
for parent in m.iter():
 for c in list(parent):
  name=c.get(a+'name','')
  if name.startswith(ad_prefix):components.append(name);parent.remove(c)
  elif name in ('com.facebook.sdk.AutoInitEnabled','com.facebook.sdk.AutoLogAppEventsEnabled','com.facebook.sdk.AdvertiserIDCollectionEnabled'):c.set(a+'value','false')
app.set(a+'label','@string/app_name');app.set(a+'usesCleartextTraffic','false')
app.set(a+'debuggable','false')
for c in app:
 if c.tag=='service' and c.get(a+'name') in ('com.sma.smartv3.biz.VoiceService','com.baidu.location.f','org.eclipse.paho.android.service.MqttService','com.sma.smartv3.ble.MyNotificationListenerService','com.sma.smartv3.screen_lock.ScreenLockService'):
  c.set(a+'enabled','false');c.set(a+'exported','false')
tree.write(target/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
for f in (target/'res').glob('values*/strings.xml'):
 s=f.read_text(encoding='utf-8');s=re.sub(r'(<string\b[^>]*name="app_name"[^>]*>).*?(</string>)',r'\g<1>워치 케어\2',s,flags=re.S);f.write_text(s,encoding='utf-8')
y=(target/'apktool.yml').read_text(encoding='utf-8');y=re.sub(r'(?m)^  versionCode:.*$','  versionCode: 10',y);y=re.sub(r'(?m)^  versionName:.*$','  versionName: 0.4.0',y);(target/'apktool.yml').write_text(y,encoding='utf-8')
# Reassemble only the DEX containing optional initializers. Other executable DEX stays byte-for-byte.
src=original/'editable/base/smali_classes19';shutil.copytree(src,target/'smali_classes19')
(target/'classes19.dex').unlink()
patched=[]
for cls,methods in [('ii0iIOi',('O00','OXO1','XO0O10','iO')),('vi6',('OXO1',)),('l87',('OXO1',))]:
 p=target/f'smali_classes19/com/android/mltcode/paycertificationapi/{cls}.smali';s=p.read_text(encoding='utf-8')
 def replace(match):
  declaration=match.group(1)
  if any(re.search(r'\b'+name+r'\(',declaration) for name in methods) and declaration.rstrip().endswith('V'):
   patched.append(cls+':'+declaration);return declaration+'\n    .locals 0\n    return-void\n.end method'
  return match.group(0)
 s=re.sub(r'(?ms)^(\.method[^\r\n]+)\r?\n.*?^\.end method',replace,s);p.write_text(s,encoding='utf-8')
# Remove SDK executable assets only from this newly created workspace.
for name in ['audience_network','ad-viewer','iads','aps-mraid.js']:
 p=target/'assets'/name
 if p.exists():
  if not p.resolve().is_relative_to(target.resolve()):raise RuntimeError('unsafe path')
  if p.is_dir():shutil.rmtree(p)
  else:p.unlink()
report={'app_id':m.get('package'),'version':'0.4.0','name':'워치 케어','mode':'original UI offline test','permissions_removed':removed,'ad_manifest_entries_removed':components,'initializer_methods_disabled':patched,'all_ad_sdk_bytecode_removed':False,'original_UI_resources_preserved':True,'network_permission':False,'note':'Optional ad/voice/audio SDK initialization disabled; dependent SDK DEX remains for compatibility. No server account features in this offline build.'}
(target.parent/'full-ui-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('Prepared',target,'removed permissions',len(removed),'removed SDK components',len(components),'patched methods',len(patched))

from pathlib import Path
import re,xml.etree.ElementTree as ET
p=target
text='%1$s 로컬 워치 테스트 버전입니다. 원본 앱의 화면과 워치 통신 기능을 유지합니다.\n\nBluetooth와 위치 권한은 주변 워치 검색 및 연결에 사용합니다. 워치에서 받은 심박과 활동 기록은 휴대폰에서 확인할 수 있습니다.\n\n이 버전은 서버 통신을 사용하지 않습니다. 계정 로그인, 온라인 워치페이스 및 날씨 기능은 연결되지 않습니다. 카메라와 통화기록 및 연락처 권한을 요청하지 않습니다.\n\n앱 삭제 시 휴대폰에 저장된 이 앱의 기록이 삭제됩니다. 의료적 진단이나 치료 효과가 검증된 제품이 아닙니다.'
for f in (p/'res').glob('values*/strings.xml'):
 s=f.read_text(encoding='utf-8')
 for key in ['privacy_content','privacy_content_no_login']:
  s=re.sub(r'(<string\b[^>]*name="'+key+r'"[^>]*>).*?(</string>)',lambda m:m[1]+text+m[2],s,flags=re.S)
 s=re.sub(r'(<string\b[^>]*name="privacy_agreement_reminder"[^>]*>).*?(</string>)',lambda m:m[1]+'로컬 테스트의 데이터 처리 안내를 확인하고 시작합니다.'+m[2],s,flags=re.S)
 f.write_text(s,encoding='utf-8')
a='{http://schemas.android.com/apk/res/android}';ET.register_namespace('android',a[1:-1]);tree=ET.parse(p/'AndroidManifest.xml');m=tree.getroot()
for name in ('ACCESS_NETWORK_STATE','ACCESS_WIFI_STATE'):
 if not any(e.get(a+'name')=='android.permission.'+name for e in m.findall('uses-permission')):ET.SubElement(m,'uses-permission',{a+'name':'android.permission.'+name})
tree.write(p/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
print('Local privacy text and network-state queries updated; INTERNET remains absent')


# Keep original UI filter while allowing named, nearby devices already bonded by the user.
source18=original/'editable/base/smali_classes18'
dest18=target/'smali_classes18'
shutil.copytree(source18,dest18)
(target/'classes18.dex').unlink()
filter_overlay=Path(__file__).resolve().parent.parent/'patches/original-ui/smali_classes18/com/sma/smartv3/ui/device/AddDeviceActivity$O00.smali'
shutil.copyfile(filter_overlay,dest18/'com/sma/smartv3/ui/device/AddDeviceActivity$O00.smali')
