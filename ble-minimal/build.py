"""Build this standalone BLE-only test app with official Android tools and a local test key."""
import argparse, os, pathlib, subprocess, zipfile, shutil
p=argparse.ArgumentParser()
for name in ['android-jar','aapt2','d8-jar','zipalign','apksigner-jar','keystore','output','build-dir']:p.add_argument('--'+name,required=True)
a=p.parse_args();source=pathlib.Path(__file__).resolve().parent
build=pathlib.Path(a.build_dir).resolve();build.mkdir(parents=True,exist_ok=True)
classes=build/'classes';classes.mkdir(exist_ok=True);dex=build/'dex';dex.mkdir(exist_ok=True)
def run(args):subprocess.check_call([str(x) for x in args])
run(['javac','-encoding','UTF-8','-source','8','-target','8','-classpath',a.android_jar,'-d',classes]+list(source.glob('src/**/*.java')))
jar=build/'classes.jar'
with zipfile.ZipFile(str(jar),'w',zipfile.ZIP_DEFLATED) as z:
 for f in classes.glob('**/*.class'):z.write(str(f),str(f.relative_to(classes)).replace('\\','/'))
run(['java','-Xmx2g','-cp',a.d8_jar,'com.android.tools.r8.D8','--min-api','26','--lib',a.android_jar,'--output',dex,jar])
resource=build/'resources.apk'
run([a.aapt2,'link','-o',resource,'-I',a.android_jar,'--manifest',source/'AndroidManifest.xml','--min-sdk-version','26','--target-sdk-version','36','--version-code','2','--version-name','0.2.0'])
unsigned=build/'unsigned.apk';shutil.copyfile(str(resource),str(unsigned))
with zipfile.ZipFile(str(unsigned),'a') as z:
 for f in dex.glob('*.dex'):z.write(str(f),f.name,compress_type=zipfile.ZIP_STORED)
aligned=build/'aligned.apk'
run([a.zipalign,'-f','-P','16','4',unsigned,aligned])
output=pathlib.Path(a.output).resolve();output.parent.mkdir(parents=True,exist_ok=True)
# Development key only. Keep keystore private and use a separate release-signing workflow for production.
password=os.environ.get('DIZ_TEST_STOREPASS','android')
run(['java','-jar',a.apksigner_jar,'sign','--ks',a.keystore,'--ks-key-alias','androiddebugkey','--ks-pass','pass:'+password,'--key-pass','pass:'+password,'--out',output,aligned])
run(['java','-jar',a.apksigner_jar,'verify','--verbose',output])
run([a.zipalign,'-c','-P','16','4',output])
print('APK ready:',output)
