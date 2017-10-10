import sys
import os
import shutil

print("fakedInterpolation is started")
print(sys.argv)
print(os.getcwd())

fileOutputName=sys.argv[3]
shutil.copy('interpolated_template.json', fileOutputName)
exit(0)
