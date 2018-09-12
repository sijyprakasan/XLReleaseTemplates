// Exported from:        http://Sijys-MacBook-Pro.local:5516/#/templates/Folder87a7ec665bad437d8de97adc072fd5e5-Release3e55042cbe6647b090d29cce802591dc/releasefile
// XL Release version:   8.2.0
// Date created:         Wed Sep 12 21:50:12 AEST 2018

xlr {
  template('01_PetPortalJiraServiceNowEnv_Prod') {
    folder('PetPortal')
    variables {
      stringVariable('buildNumber') {
        required false
        showOnReleaseStart false
      }
      stringVariable('ticket') {
        required false
        showOnReleaseStart false
      }
      stringVariable('changeRequestId') {
        required false
        showOnReleaseStart false
      }
      stringVariable('changeRequestSysId') {
        required false
        showOnReleaseStart false
      }
      stringVariable('changeRequestStatus') {
        required false
        showOnReleaseStart false
      }
      mapVariable('JiraIssue') {
        required false
        showOnReleaseStart false
        label 'List of Jira Issues'
        description 'List of associated Jira issues'
      }
      stringVariable('changeRequestIdProd') {
        required false
        showOnReleaseStart false
        label 'Change Request reference for the Prod change from ServiceNow'
      }
      stringVariable('changeRequestSysIdProd') {
        required false
        showOnReleaseStart false
      }
    }
    description 'Automated Release for PetPortal application to PROD with Jira, jenkins, XLDeploy and ServiceNow'
    scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2016-02-02T19:00:00+1100')
    realFlagStatus com.xebialabs.xlrelease.domain.status.FlagStatus.ATTENTION_NEEDED
    tags 'PetPortal', 'Customerservices'
    phases {
      phase('Dev_QA') {
        color '#009CDB'
        tasks {
          custom('Get Issue from Jira') {
            script {
              type 'jira.Query'
              query 'PROJECT ="CatalinkDev_ServiceDesk" AND status = "open"'
              issues variable('JiraIssue')
            }
          }
          custom('Open Change Request') {
            script {
              type 'servicenow.CreateRequest'
              shortDescription 'PetPortal Demo'
              comments 'PetPortal Demo'
              sysId variable('changeRequestSysId')
              'Ticket' variable('changeRequestId')
            }
          }
          custom('Build package') {
            script {
              type 'jenkins.Build'
              jobName 'PetPortal'
              buildNumber variable('buildNumber')
            }
          }
          custom('Verify Source Code Quality') {
            flagStatus com.xebialabs.xlrelease.domain.status.FlagStatus.ATTENTION_NEEDED
            flagComment 'Task \'Verify Source Code Quality\' in Phase \'Dev_QA\' has been replaced by a manual task. The task of type \'sonar.VerifyQuality\' could not be found because of a missing plugin.'
            script {
              type 'sonar.checkCompliance'
              resource 'org.springframework.samples:spring-petclinic'
            }
          }
          custom('Acquire QA Environment') {
            scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2018-08-15T19:04:00+1000')
            script {
              type 'delivery.mark'
              component 'PetPortal'
              environment 'Env_QA_Petportal'
            }
          }
          custom('Deploy to dev') {
            script {
              type 'xldeploy.Deploy'
              retryCounter 'currentContinueRetrial':'0','currentPollingTrial':'0'
              deploymentPackage 'PetPortal/2.1-${buildNumber}'
              deploymentEnvironment 'Dev/DEV'
            }
          }
          custom('Release QA Environment') {
            scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2018-08-20T19:06:00+1000')
            script {
              type 'delivery.unmark'
              component 'PetPortal'
              environment 'Env_QA_Petportal'
            }
          }
          custom('Notify Team on Slack') {
            script {
              type 'slack.Notification'
              channel '#devops'
              message 'PetPortal/2.1-${buildNumber} deployed successfully and available on Dev'
            }
          }
          custom('Add comment to change request') {
            script {
              type 'servicenow.UpdateRecord'
              sysId '${changeRequestSysId}'
              content '{\n' +
                      '"work_notes":"PetPortal/2.1-${buildNumber} available on Dev and is resolving Jira issue ${JiraIssue} " \n' +
                      '}'
            }
          }
        }
      }
      phase('UAT') {
        color '#08B153'
        tasks {
          custom('Wait for approval for deployment to test') {
            script {
              type 'servicenow.PollingCheckStatus'
              sysId '${changeRequestSysId}'
              pollInterval 10
              statusField 'approval'
              checkForStatus 'Approved'
              status variable('changeRequestStatus')
            }
          }
          gate('Approve Deployment') {
            owner 'Robert'
          }
          custom('Acquire UAT Environment') {
            script {
              type 'delivery.mark'
              component 'PetPortal'
              environment 'Env_ACC_Petportal'
            }
          }
          custom('Deploy to UAT') {
            script {
              type 'xldeploy.Deploy'
              retryCounter 'currentContinueRetrial':'0','currentPollingTrial':'0'
              deploymentPackage 'PetPortal/2.1-${buildNumber}'
              deploymentEnvironment 'Ops/Acc/ACC'
            }
          }
          parallelGroup('Perform Automated Testing') {
            tasks {
              script('Perform Smoke Tests') {
                script 'import time\n' +
                       'time.sleep(5)\n' +
                       'print "Performing Smoke Tests"'
              }
              script('Perform Load Tests') {
                script 'import time\n' +
                       'time.sleep(10)\n' +
                       'print "Performing  Load Tests"'
              }
            }
          }
          gate('Verify Test Deployment') {
            owner 'Patrick'
          }
          custom('Release UAT Environment') {
            script {
              type 'delivery.unmark'
              component 'PetPortal'
              environment 'Env_ACC_Petportal'
            }
          }
          custom('Update ServiceNow: Change Request') {
            script {
              type 'servicenow.UpdateChangeRequest'
              sysId '${changeRequestSysId}'
              content '{\n' +
                      '"State":"3",\n' +
                      '"Close_code":"Successful",\n' +
                      '"Close_notes":"Completed"\n' +
                      '}'
            }
          }
          custom('Update Jira') {
            description 'Update Jira with completion summary with ServiceNow Change ID'
            script {
              type 'jira.UpdateIssues'
              issues variable('JiraIssue')
              newStatus 'Completed'
              comment 'Test completed successfully : Issue resolved with deployment of new version ${buildNumber} to Test environment and approved by change  ${changeRequestId}'
            }
          }
          custom('Notify Team on Slack') {
            script {
              type 'slack.Notification'
              channel '#devops'
              message 'PetPortal/2.1-${buildNumber} deployed successfully and available on UAT'
            }
          }
        }
      }
      phase('PROD') {
        color '#D94C3D'
        tasks {
          custom('Open Change Request') {
            script {
              type 'servicenow.CreateRequest'
              shortDescription 'PetPortal Demo'
              comments 'PetPortal Demo'
              sysId variable('changeRequestSysIdProd')
              'Ticket' variable('changeRequestIdProd')
            }
          }
          custom('Wait for approval for deployment to PROD') {
            script {
              type 'servicenow.PollingCheckStatus'
              sysId '${changeRequestSysIdProd}'
              pollInterval 10
              statusField 'approval'
              checkForStatus 'Approved'
              status variable('changeRequestStatus')
            }
          }
          custom('Deploy to PROD') {
            script {
              type 'xldeploy.Deploy'
              retryCounter 'currentContinueRetrial':'0','currentPollingTrial':'0'
              deploymentPackage 'PetPortal/2.1-${buildNumber}'
              deploymentEnvironment 'Ops/Prod/PROD'
            }
          }
          custom('Notify Team on Prod deployment') {
            script {
              type 'slack.Notification'
              channel '#devops'
              message 'PetPortal/2.1-${buildNumber} deployed successfully and available on Prod for Smoke Tests'
            }
          }
          gate('Verify Prod Deployment and Smoke Tests') {
            owner 'Patrick'
          }
          custom('Update ServiceNow: Change Request') {
            script {
              type 'servicenow.UpdateChangeRequest'
              sysId '${changeRequestSysIdProd}'
              content '{\n' +
                      '"State":"3",\n' +
                      '"Close_code":"Successful",\n' +
                      '"Close_notes":"Completed"\n' +
                      '}'
            }
          }
          custom('Update Jira') {
            description 'Update Jira with completion summary with ServiceNow Change ID'
            script {
              type 'jira.UpdateIssues'
              issues variable('JiraIssue')
              newStatus 'Closed'
              comment 'Issue resolved with deployment of new version ${buildNumber} to Production and approved by change  ${changeRequestIdProd}'
            }
          }
          custom('Notify Team on Prod Release complete') {
            script {
              type 'slack.Notification'
              channel '#devops'
              message 'PetPortal/2.1-${buildNumber} deployed successfully and live on PROD, Thanks for the wonderful team efforts ! Cheers !'
            }
          }
        }
      }
    }
    extensions {
      dashboard('Dashboard') {
        tiles {
          releaseProgressTile('Release progress') {
            
          }
          releaseHealthTile('Release health') {
            
          }
          releaseSummaryTile('Release summary') {
            
          }
          resourceUsageTile('Resource usage') {
            row 4
          }
          timelineTile('Release timeline') {
            
          }
          jenkinsBuildsTile('Jenkins builds') {
            row 2
            col 1
          }
          xLDeployTile('XL Deploy deployments') {
            row 2
            col 2
          }
          jiraQueryTile('JIRA issues') {
            row 2
            col 0
            query 'PROJECT ="CatalinkDev_ServiceDesk" AND status = "open"'
            supportedScopes 'global', 'folder', 'release'
          }
          serviceNowQueryTile('ServiceNow Incidents') {
            row 3
            col 1
            detailsViewColumns 'number':'number','short_description':'short_description','state':'state','priority':'priority','assigned_to':'assigned_to.display_value'
          }
          serviceNowQueryTile('ServiceNow Changes') {
            row 3
            col 0
            tableName 'change_request'
            detailsViewColumns 'number':'number','short_description':'short_description','state':'state','priority':'priority','assigned_to':'assigned_to.display_value'
          }
        }
      }
    }
    
  }
}