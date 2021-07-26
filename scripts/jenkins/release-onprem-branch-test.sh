#!/bin/bash

set -e

PROJECTS="ART|BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PL|SEC|SWAT|GTM|ONP"

cat <<EOF > ${HOME}/.netrc
machine ${DRONE_NETRC_MACHINE}
login ${DRONE_NETRC_USERNAME}
password ${DRONE_NETRC_PASSWORD}
EOF


git fetch --unshallow

git fetch --all


for line in `git branch -r | grep "release/on-prem" |grep ".xx$"| tail -${PREV_BRANCHES_COUNT}`;
	do
	       echo "Onprem branch is $line"
         git log --remotes=$line* --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq | tr '[:lower:]' '[:upper:]' >> release_onprem_temp.txt
done

cat release_onprem_temp.txt | sort | uniq >release_onprem.txt


echo "Saas branch is ${SAAS_BRANCH}"

git log --remotes=origin/${SAAS_BRANCH}* --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq | tr '[:lower:]' '[:upper:]' > release_saas.txt

cat release_onprem.txt | wc -l
cat release_saas.txt | wc -l


NOT_MERGED=`comm -23 release_onprem.txt release_saas.txt | tr '\n' ' '`



if [ -z "$NOT_MERGED" ]
then
      echo "All onprem hotfixes are exist in ${SAAS_BRANCH} saas branch "
else
      echo "These are the not merged JIRA tickets : ${NOT_MERGED} , Please merge them into ${SAAS_BRANCH} branch"
      exit 1
fi