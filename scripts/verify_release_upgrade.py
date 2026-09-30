"""Exercise same-key version 4 -> 5 replacement without clearing app data, emulator only."""
import json,os,subprocess
from pathlib import Path
from release_version import version

def run(args): return subprocess.check_output(args,text=True,stderr=subprocess.STDOUT)
if run(['adb','shell','getprop','ro.kernel.qemu']).strip()!='1': raise SystemExit('Disposable emulator required')
package='io.github.ahmed9461.tapsave'
if 'package:' in subprocess.run(['adb','shell','pm','path',package],capture_output=True,text=True).stdout:
    raise SystemExit('Refusing to replace a pre-existing app during this test')
run(['adb','shell','content','query','--uri','content://media/external/video/media','--projection','_id'])
run(['adb','shell','content','call','--uri','content://media','--method','wait_for_idle'])
run(['adb','install',str(Path(os.environ['RUNNER_TEMP'])/'tap-save-baseline.apk')])
run(['adb','install','-t',str(Path(os.environ['RUNNER_TEMP'])/'signed-tests.apk')])
code,_=version()
for phase in ('seed','verify'):
    if phase=='verify':
        result=run(['adb','install','-r','app/build/outputs/apk/release/app-release.apk'])
        if 'Success' not in result: raise SystemExit('Update installation failed')
    result=run(['adb','shell','am','instrument','-w','-e','class',package+'.ReleaseUpgradeTest','-e','upgradePhase',phase,
                '-e','expectedVersion',str(code-1 if phase=='seed' else code),
                package+'.test/io.github.ahmed9461.tapsave.TapSaveTestRunner'])
    Path('app/build/reports/upgrade-'+phase+'.txt').write_text(result)
    if 'OK (1 test)' not in result or 'FAILURES!!!' in result: raise SystemExit(result)
Path('app/build/reports/upgrade-verification.json').write_text(json.dumps({'baseline_version_code':code-1,'candidate_version_code':code,
    'same_key':True,'install_replace':True,'preferences_retained':True,'media_bytes_retained':True,'non_debuggable':True},indent=2)+'\n')
print(f'Signed release update passed: version {code-1} -> {code}, preferences and owned media retained.')
