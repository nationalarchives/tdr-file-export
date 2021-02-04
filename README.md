## Consignment Export
This is the project which contains the code for the consignment export. 

The infrastructure for the export, is defined in the [terraform]("https://github.com/nationalarchives/tdr-terraform-environments") repository.
* The front end makes a request to the api gateway with the user's token
* A lambda authorises the request using the consignment api to check the token
* Api gateway triggers a step function
* The step function triggers an ECS task which runs the code in this repository.

There are two sub-modules here. 
The code in the exporter module does the following:
* Calls the API to get a list of files
* Downloads the files from S3 to the EFS volume
* Creates the Bagit bag
* Tar and zip the bag
* Create a checksum file for the tar file
* Upload to the export bucket.
* Update the API with the export location

The code in the authoriser module:
* Gets the token from the request and the consignment ID from the path
* Calls the API with the token. If the consignment is returned then the user is authorised and Allow is returned, otherwise Deny is returned.

### Running Locally

Set the `CLIENT_SECRET` environment variable to the client secret for the tdr-backend-checks Keycloak client in the integration environment, which you can find by logging into Keycloak or by running the AWS CLI with intg credentials:

```
aws ssm get-parameters  --names "/intg/keycloak/backend_checks_client/secret"  --with-decryption
```

You can run the Main object in Intellij as you can with any similar project. You will need to provide the program arguments: `export --consignmentId {a uuid}`

You can also run `sbt universal:packageZipTarball` which creates a file `target/universal/tdr-consignment-export.tgz` which, when unzipped, gives you a `bin/tdr-consignment-export` executable which you can run with the same arguments as above. This is how the docker container runs the file so is the closest to running this in production.

### Deployment
Because this is run as an on demand ECS task, deployment is just pushing a new version of the docker image to ECR with the appropriate stage tag. This will be done by Jenkins as part of the deploy job on merge to master so there should be no reason to do this locally.
