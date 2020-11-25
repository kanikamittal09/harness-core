"""
Generated by command:
bq show --format=prettyjson ce-prod-274307:BillingReport_whejvm7ntje2fz99pdo2ya.unifiedTable | jq
"""

unifiedTableTableSchema = [
      {
        "mode": "REQUIRED",
        "name": "startTime",
        "type": "TIMESTAMP"
      },
      {
        "mode": "NULLABLE",
        "name": "cost",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "gcpProduct",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "gcpSkuId",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "gcpSkuDescription",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "gcpProjectId",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "region",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "zone",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "gcpBillingAccountId",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "cloudProvider",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "awsBlendedRate",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "awsBlendedCost",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "awsUnblendedRate",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "awsUnblendedCost",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "awsServicecode",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "awsAvailabilityzone",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "awsUsageaccountid",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "awsInstancetype",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "awsUsagetype",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "discount",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "endtime",
        "type": "TIMESTAMP"
      },
      {
        "mode": "NULLABLE",
        "name": "accountid",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "instancetype",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "clusterid",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "clustername",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "appid",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "serviceid",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "envid",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "cloudproviderid",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "launchtype",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "clustertype",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "workloadname",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "workloadtype",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "namespace",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "cloudservicename",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "taskid",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "clustercloudprovider",
        "type": "STRING"
      },
      {
        "mode": "NULLABLE",
        "name": "billingamount",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "cpubillingamount",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "memorybillingamount",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "idlecost",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "maxcpuutilization",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "avgcpuutilization",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "systemcost",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "actualidlecost",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "unallocatedcost",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "networkcost",
        "type": "FLOAT"
      },
      {
        "mode": "NULLABLE",
        "name": "product",
        "type": "STRING"
      },
      {
        "fields": [
          {
            "name": "key",
            "type": "STRING"
          },
          {
            "name": "value",
            "type": "STRING"
          }
        ],
        "mode": "REPEATED",
        "name": "labels",
        "type": "RECORD"
      }
    ]