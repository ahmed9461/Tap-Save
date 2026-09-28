"""Reject unsigned/wrong-signer/debuggable APKs. Persist only public build metadata."""
import hashlib,json,os,re,subprocess,sys
from pathlib import Path
from release_version import version

apk=Path(sys.argv[1])
tools=Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
output=subprocess.check_output([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
certificate=re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)',output).group(1)
expected=Path('signing/certificate-sha256.txt').read_text().strip()
if certificate != expected: raise SystemExit('Release signing certificate mismatch')
manifest=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True)
package=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",manifest)
code,name=version()
if not package or package.groups()!=('io.github.ahmed9461.tapsave',str(code),name): raise SystemExit('Wrong package/version')
if 'application-debuggable' in manifest: raise SystemExit('Release must not be debuggable')
result={'source':os.environ.get('GITHUB_SHA'),'version_code':code,'version_name':name,'bytes':apk.stat().st_size,
        'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'certificate_sha256':certificate,'debuggable':False}
destination=Path('app/build/reports/release-verification.json');destination.parent.mkdir(parents=True,exist_ok=True)
destination.write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
