import os
import re

dir_path = r'E:\JAGallery\app\src\main\java\com\aktarjabed\jagallery\ui\screens'

# Regex for WhileSubscribed
state_in_pattern = re.compile(r'\.stateIn\(\s*viewModelScope\s*,\s*SharingStarted\.(?:Lazily|Eagerly|WhileSubscribed\([^)]*\))\s*,')

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # 1. SharingStarted.WhileSubscribed(5000)
    content = re.sub(
        r'\.stateIn\(\s*viewModelScope\s*,\s*SharingStarted\.(?:Lazily|Eagerly|WhileSubscribed\([^)]*\))\s*,',
        r'.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),',
        content
    )
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, _, files in os.walk(dir_path):
    for file in files:
        if file.endswith('ViewModel.kt'):
            process_file(os.path.join(root, file))
