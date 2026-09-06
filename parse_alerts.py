import json
import subprocess

result = subprocess.run(
    ['gh', 'api', 'repos/shuiyind/touhou-little-maid-command-processor/code-scanning/alerts'],
    capture_output=True, text=True
)

alerts = json.loads(result.stdout)

for a in alerts:
    num = a['number']
    rule = a['rule']['id']
    rule_desc = a['rule'].get('description', '')
    sev = a['rule']['severity']
    state = a['state']
    instance = a['most_recent_instance']
    file_path = instance['location']['path']
    line = instance.get('start_line', 'N/A')
    snippet = instance.get('snippet', '')
    
    print(f'Alert #{num} | {rule} [{sev}] | {state}')
    print(f'  File: {file_path}:{line}')
    print(f'  Desc: {rule_desc}')
    if snippet:
        print(f'  Code: {snippet}')
    print()
