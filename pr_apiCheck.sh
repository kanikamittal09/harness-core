git fetch origin $1:$1
git checkout $1
echo $2 
commit_id=$(git merge-base $1 $2) 
git checkout $2
git checkout $commit_id

echo "=============TARGET BRANCH================"
echo "BUILD NG_MANAGER"
NG_MANAGER_T=0
bazel build ${BAZEL_ARGS} -- //120-ng-manager:module_deploy.jar || NG_MANAGER_T=$?
echo "BUILD DASHBOARD_SERVICE"
DASHBOARD_SERVICE_T=0
bazel build ${BAZEL_ARGS} -- //290-dashboard-service:module_deploy.jar || DASHBOARD_SERVICE_T=$?
echo "BUILD CI_MANAGER"
CI_MANAGER_T=0
bazel build ${BAZEL_ARGS} -- //310-ci-manager:module_deploy.jar || CI_MANAGER_T=$?
echo "BUILD CE_NEXTGEN"
CE_NEXTGEN_T=0
bazel build ${BAZEL_ARGS} -- //340-ce-nextgen:module_deploy.jar || CE_NEXTGEN_T=$?
echo "BUILD PIPELINE_SERVICE"
PIPELINE_SERVICE_T=0
bazel build ${BAZEL_ARGS} -- //800-pipeline-service:module_deploy.jar || PIPELINE_SERVICE_T=$? 
echo "BUILD TEMPLATE_SERVICE"
TEMPLATE_SERVICE_T=0
bazel build ${BAZEL_ARGS} -- //840-template-service:module_deploy.jar || TEMPLATE_SERVICE_T=$?
echo "BUILD PLATFORM_SERVICE"            
PLATFORM_SERVICE_T=0           
bazel build ${BAZEL_ARGS} -- //platform-service/service:module_deploy.jar || PLATFORM_SERVICE_T=$?
echo "BUILD ACCESS_CONTROL"
ACCESS_CONTROL_T=0         
bazel build ${BAZEL_ARGS} -- //access-control/service:module_deploy.jar || ACCESS_CONTROL_T=$?

mkdir target

touch target/120_target.json
touch target/290_target.json
touch target/310_target.json
touch target/340_target.json
touch target/800_target.json
touch target/840_target.json
touch target/platform_target.json
touch target/access_target.json

pwd
ls
echo "=============GENERATE TARGET BRANCH API SPEC================"
if [ $NG_MANAGER_T -eq 0 ]                                                                                                      
then
    echo "====Generating NG_Manager Target-Branch Api Spec===="
    java -jar bazel-bin/120-ng-manager/module_deploy.jar generate-openapi-spec target/120_target.json || NG_MANAGER_T=$?
fi

if [ $DASHBOARD_SERVICE_T -eq 0 ]                                                                                                      
then
    echo "====Generating Dashboard_Service Target-Branch Api Spec===="
    java -jar bazel-bin/290-dashboard-service/module_deploy.jar generate-openapi-spec target/290_target.json || DASHBOARD_SERVICE_T=$?
fi

if [ $CI_MANAGER_T -eq 0 ]                                                                                                      
then
    echo "====Generating CI-Manager Target-Branch Api Spec===="
    java -jar bazel-bin/310-ci-manager/module_deploy.jar generate-openapi-spec target/310_target.json  310-ci-manager/ci-manager-config.yml || CI_MANAGER_T=$?
fi

if [ $CE_NEXTGEN_T -eq 0 ]                                                                                                      
then
    echo "====Generating CE-NextGen Target-Branch Api Spec===="
    java -jar bazel-bin/340-ce-nextgen/module_deploy.jar generate-openapi-spec target/340_target.json || CE_NEXTGEN_T=$?
fi

if [ $PIPELINE_SERVICE_T -eq 0 ]                                                                                                      
then
    echo "====Generating Pipeline-Service Target-Branch Api Spec===="
    java -jar bazel-bin/800-pipeline-service/module_deploy.jar generate-openapi-spec target/800_target.json || PIPELINE_SERVICE_T=$?
fi

if [ $TEMPLATE_SERVICE_T -eq 0 ]                                                                                                      
then
    echo "====Generating Template-Service Target-Branch Api Spec===="
    java -jar bazel-bin/840-template-service/module_deploy.jar generate-openapi-spec target/840_target.json || TEMPLATE_SERVICE_T=$?
fi

if [ $PLATFORM_SERVICE_T -eq 0 ]                                                                                                      
then
    echo "====Generating Platform-Service Target-Branch Api Spec===="
    java -jar bazel-bin/platform-service/service/module_deploy.jar generate-openapi-spec target/platform_target.json || PLATFORM_SERVICE_T=$?
fi

if [ $ACCESS_CONTROL_T -eq 0 ]                                                                                                      
then
    echo "====Generating Access-Control Target-Branch Api Spec===="
    java -jar bazel-bin/access-control/service/module_deploy.jar generate-openapi-spec target/access_target.json || ACCESS_CONTROL_T=$?
fi



echo "=============SOURCE BRANCH================"
git checkout $3
last_commit=$(git log -1 --format="%H")
email_id=$(git show $last_commit | grep Author | cut -d '<' -f2- | sed 's/.$//')
export EMAIL_ID=$email_id

if [ $NG_MANAGER_T -eq 0 ]
then 
    echo "BUILD NG_MANAGER"
    NG_MANAGER_S=0
    bazel build ${BAZEL_ARGS} -- //120-ng-manager:module_deploy.jar || NG_MANAGER_S=$?
else
    NG_MANAGER_S=1
fi

if [ $DASHBOARD_SERVICE_T -eq 0 ]
then 
    echo "BUILD DASHBOARD_SERVICE"
    DASHBOARD_SERVICE_S=0
    bazel build ${BAZEL_ARGS} -- //290-dashboard-service:module_deploy.jar || DASHBOARD_SERVICE_S=$?
else
    DASHBOARD_SERVICE_S=1
fi

if [ $CI_MANAGER_T -eq 0 ]
then 
    echo "BUILD CI_MANAGER"
    CI_MANAGER_S=0
    bazel build ${BAZEL_ARGS} -- //310-ci-manager:module_deploy.jar || CI_MANAGER_S=$?
else
    CI_MANAGER_S=1
fi

if [ $CE_NEXTGEN_T -eq 0 ]
then
    echo "BUILD CE_NEXTGEN"
    CE_NEXTGEN_S=0
    bazel build ${BAZEL_ARGS} -- //340-ce-nextgen:module_deploy.jar || CE_NEXTGEN_S=$?
else
    CE_NEXTGEN_S=1
fi 

if [ $PIPELINE_SERVICE_T -eq 0 ]
then
    echo "BUILD PIPELINE_SERVICE"
    PIPELINE_SERVICE_S=0
    bazel build ${BAZEL_ARGS} -- //800-pipeline-service:module_deploy.jar || PIPELINE_SERVICE_S=$?
else
    PIPELINE_SERVICE_S=1
fi 

if [ $TEMPLATE_SERVICE_T=0 ]
then
    echo "BUILD TEMPLATE_SERVICE"
    TEMPLATE_SERVICE_S=0
    bazel build ${BAZEL_ARGS} -- //840-template-service:module_deploy.jar || TEMPLATE_SERVICE_S=$?
else
    TEMPLATE_SERVICE_S=1
fi

if [ $PLATFORM_SERVICE_T=0 ]
then
    echo "BUILD PLATFORM_SERVICE"            
    PLATFORM_SERVICE_S=0           
    bazel build ${BAZEL_ARGS} -- //platform-service/service:module_deploy.jar || PLATFORM_SERVICE_S=$?
else
    PLATFORM_SERVICE_S=1
fi

if [ $ACCESS_CONTROL_T=0 ]
then
    echo "BUILD ACCESS_CONTROL"
    ACCESS_CONTROL_S=0         
    bazel build ${BAZEL_ARGS} -- //access-control/service:module_deploy.jar || ACCESS_CONTROL_S=$?
else
    ACCESS_CONTROL_S=0  
fi

touch target/120_source.json
touch target/290_source.json
touch target/310_source.json
touch target/340_source.json
touch target/800_source.json
touch target/840_source.json
touch target/platform_source.json
touch target/access_source.json

pwd
ls
echo "=============GENERATE SOURCE BRANCH API SPEC================"
if [ $NG_MANAGER_S -eq 0 ]                                                                                                   
then
    echo "====Generating NG-Manager Source-Branch Api Spec===="
    java -jar bazel-bin/120-ng-manager/module_deploy.jar generate-openapi-spec target/120_source.json || NG_MANAGER_S=$?
fi

if [ $CI_MANAGER_S -eq 0 ]                                                                                                    
then
    echo "====Generating CI-Manager Source-Branch Api Spec===="
    java -jar bazel-bin/310-ci-manager/module_deploy.jar generate-openapi-spec target/310_source.json 310-ci-manager/ci-manager-config.yml || CI_MANAGER_S=$?
fi

if [ $DASHBOARD_SERVICE_S -eq 0 ]                                                                                             
then
    echo "====Generating Dashboard-Service Source-Branch Api Spec===="
    java -jar bazel-bin/290-dashboard-service/module_deploy.jar generate-openapi-spec target/290_source.json || DASHBOARD_SERVICE_S=$?
fi

if [ $CE_NEXTGEN_S -eq 0 ]                                                                                              
then
    echo "====Generating CE-NextGen Source-Branch Api Spec===="
    java -jar bazel-bin/340-ce-nextgen/module_deploy.jar generate-openapi-spec target/340_source.json || CE_NEXTGEN_S=$?
fi

if [ $PIPELINE_SERVICE_S -eq 0 ]                                                                                             
then
    echo "====Generating Pipeline-Service Source-Branch Api Spec===="
    java -jar bazel-bin/800-pipeline-service/module_deploy.jar generate-openapi-spec target/800_source.json || PIPELINE_SERVICE_S=$?
fi

if [ $TEMPLATE_SERVICE_S -eq 0 ]                                                                                                      
then
    echo "====Generating Template-Service Source-Branch Api Spec===="
    java -jar bazel-bin/840-template-service/module_deploy.jar generate-openapi-spec target/840_source.json || TEMPLATE_SERVICE_S=$?
fi

if [ $PLATFORM_SERVICE_S -eq 0 ]                                                                                                 
then
    echo "====Generating Platform-Service Source-Branch Api Spec===="
    java -jar bazel-bin/platform-service/service/module_deploy.jar generate-openapi-spec target/platform_source.json || PLATFORM_SERVICE_S=$?
fi

if [ $ACCESS_CONTROL_S -eq 0 ]                                                                                                   
then
    echo "====Generating Access-Control Source-Branch Api Spec===="
    java -jar bazel-bin/access-control/service/module_deploy.jar generate-openapi-spec target/access_source.json || ACCESS_CONTROL_S=$?
fi

exit_code=0
issues=""
echo "=============API BACKWARD COMPATIBILITY CHECKS================"
rc=0
echo 120-NG-MANAGER
if [[ $NG_MANAGER_S -eq 0 ]] && [[ $NG_MANAGER_T -eq 0 ]]
then    
    java -jar with_handle_schema.jar target/120_target.json target/120_source.json --fail-on-incompatible || rc=$?
    if [ $rc -eq 1 ]                                                                                                      
    then
        exit_code=1
        issues+="120-NG-MANAGER "
    fi 
fi

rc=0
echo 290-DASHBOARD-SERVICE
if [[ $DASHBOARD_SERVICE_S -eq 0 ]] && [[ $DASHBOARD_SERVICE_T -eq 0 ]]
then
    java -jar with_handle_schema.jar target/290_target.json target/290_source.json --fail-on-incompatible || rc=$?
    if [ $rc -eq 1 ]                                                                                                      
    then
        exit_code=1
        issues+="290-DASHBOARD-SERVICE "
    fi
fi 

rc=0
echo 310-CI-MANAGER
if [[ $CI_MANAGER_S -eq 0 ]] && [[ $CI_MANAGER_T -eq 0 ]]
then
    java -jar with_handle_schema.jar target/310_target.json target/310_source.json --fail-on-incompatible || rc=$?
    if [ $rc -eq 1 ]                                                                                                      
    then
        exit_code=1
        issues+="310-CI-MANAGER "
    fi
fi 

rc=0
echo 340-CE-NEXTGEN
if [[ $CE_NEXTGEN_S -eq 0 ]] && [[ $CE_NEXTGEN_T -eq 0 ]]
then
    java -jar with_handle_schema.jar target/340_target.json target/340_source.json --fail-on-incompatible || rc=$?
    if [ $rc -eq 1 ]                                                                                                      
    then
        exit_code=1
        issues+="340-CE-NEXTGEN "
    fi 
fi

rc=0    
echo 800-PIPELINE-SERVICE
if [[ $PIPELINE_SERVICE_S  -eq 0 ]] && [[ $PIPELINE_SERVICE_T  -eq 0 ]]
then
    java -jar with_handle_schema.jar target/800_target.json target/800_source.json --fail-on-incompatible || rc=$?
    if [ $rc -eq 1 ]                                                                                                      
    then
        exit_code=1
        issues+="800-PIPELINE-SERVICE "
    fi
fi    

rc=0
echo 840-TEMPLATE-SERVICE 
if [[ $TEMPLATE_SERVICE_S -eq 0 ]] && [[ $TEMPLATE_SERVICE_T -eq 0 ]]
then
    java -jar with_handle_schema.jar target/840_target.json target/840_source.json --fail-on-incompatible || rc=$?
    if [ $rc -eq 1 ]                                                                                                      
    then
        exit_code=1
        issues+="840-TEMPLATE-SERVICE "
    fi
fi 

rc=0
echo PLATFORM-SERVICE
if [[ $PLATFORM_SERVICE_S -eq 0 ]] && [[ $PLATFORM_SERVICE_T  -eq 0 ]]
then
    java -jar with_handle_schema.jar target/platform_target.json target/platform_source.json --fail-on-incompatible || rc=$?
    if [ $rc -eq 1 ]                                                                                                      
    then
        exit_code=1
        issues+="PLATFORM-SERVICE "
    fi 
fi

rc=0
echo ACCESS-CONTROL
if [[ $ACCESS_CONTROL_S -eq 0 ]] && [[ $ACCESS_CONTROL_T -eq 0 ]]
then
    java -jar with_handle_schema.jar target/access_target.json target/access_source.json --fail-on-incompatible || rc=$?
    if [ $rc -eq 1 ]                                                                                                      
    then
        exit_code=1
        issues+="ACCESS-CONTROL "
    fi
fi
echo "API Backward Incompatibility issues in services :"$issues

export EXIT_CODE=$exit_code