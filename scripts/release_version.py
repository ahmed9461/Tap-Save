"""Single release version source shared with Gradle; no signing data."""
from pathlib import Path
import sys

def version():
    values=dict(line.split('=',1) for line in Path('app/version.properties').read_text().splitlines() if '=' in line)
    return int(values['versionCode']),values['versionName']

if __name__=='__main__':
    code,_=version()
    if code <= 1: raise SystemExit('Update test needs a previous positive version code')
    print(code-1 if '--previous' in sys.argv else code)
