"""Update a decoded Watch Care build without changing its installation identity."""
import argparse, json, re
from pathlib import Path
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--decoded',required=True)
p.add_argument('--version-code',required=True,type=int)
p.add_argument('--version-name',required=True)
p.add_argument('--label')
a=p.parse_args()
root=Path(a.decoded).resolve()
manifest=root/'AndroidManifest.xml'; metadata=root/'apktool.yml'
ns='{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android',ns[1:-1])
tree=ET.parse(manifest); app=tree.getroot()
if app.get('package')!='com.diz.sw01.custom':
    raise SystemExit('Expected Watch Care application ID com.diz.sw01.custom; identity changes require a separate review.')
y=metadata.read_text(encoding='utf-8-sig')
codes=re.findall(r'(?m)^  versionCode:\s*[\'"]?(\d+)',y)
if len(codes)!=1:raise SystemExit('Expected one versionCode in apktool.yml')
if a.version_code<int(codes[0]):raise SystemExit('Do not decrease versionCode below '+codes[0])
if not re.fullmatch(r'[0-9A-Za-z][0-9A-Za-z.+_-]{0,63}',a.version_name):raise SystemExit('Use a simple versionName such as 0.4.1')
if a.label is not None and (not a.label.strip() or '\n' in a.label or len(a.label)>80):raise SystemExit('Invalid label')
replacements={}
if a.label is not None:
    value=escape(a.label).replace("'",r"\'").replace('"',r'\"')
    expression=re.compile(r'(<string\b[^>]*\bname="app_name"[^>]*>).*?(</string>)',re.S)
    for f in (root/'res').glob('values*/strings.xml'):
        text=f.read_text(encoding='utf-8-sig')
        updated,n=expression.subn(lambda m:m[1]+value+m[2],text)
        if n:replacements[f]=updated
    if root/'res/values/strings.xml' not in replacements:raise SystemExit('Default app_name string missing')
y,n1=re.subn(r'(?m)^  versionCode:.*$','  versionCode: '+str(a.version_code),y)
y,n2=re.subn(r'(?m)^  versionName:.*$','  versionName: '+json.dumps(a.version_name),y)
if (n1,n2)!=(1,1):raise SystemExit('Unexpected versionInfo shape')
app.set(ns+'versionCode',str(a.version_code));app.set(ns+'versionName',a.version_name)
tree.write(manifest,encoding='utf-8',xml_declaration=True)
metadata.write_bytes(y.encode('utf-8'))
for f,text in replacements.items():f.write_bytes(text.encode('utf-8'))
print('Metadata updated:',a.version_name,'versionCode',a.version_code,'localized labels',len(replacements))
