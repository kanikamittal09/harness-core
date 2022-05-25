import json
import sys

def sort_json(json1):
    f = open(json1 , "r")
    json_ob1= json.load(f)
    f.close()
    json_sort1 = json.dumps(json_ob1, indent=4,sort_keys=True)
    with open(json1.split(".")[0]+"_new.json", "w") as outfile:
        outfile.write(json_sort1)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit('Error')
    location1 = sys.argv[1]
    sort_json(location1)
