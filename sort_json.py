import json
import sys

def sort_json(json1, json2):
    f = open(json1 , "r")
    json_ob1= json.load(f)
    f.close()
    json_sort1 = json.dumps(json_ob1, indent=4,sort_keys=True)
    with open(json1.split(".")[0]+"_new.json", "w") as outfile:
        outfile.write(json_sort1)
    f = open(json2 , "r")
    json_ob2= json.load(f)
    f.close()
    json_sort2 = json.dumps(json_ob2, indent=4,sort_keys=True)
    with open(json2.split(".")[0]+"_new.json", "w") as outfile:
        outfile.write(json_sort2)



if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit('Error')
    location1 = sys.argv[1]
    location2 = sys.argv[2]
    sort_json(location1, location2)