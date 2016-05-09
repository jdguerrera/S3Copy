# S3Copy

## Introduction
This is a simple program to test S3 multipart file uploads. I originally did this to test transfer speeds from a corporate network to S3.

## Use
This is an IntelliJ Java project utilizing the AWS SDK.

1. Compile
2. Setup AWS credentials
3. Execute Jar


    $ java -jar ./S3Copy.jar -bucket bucket-name-here -file /Path/To/Filename.txt -key Filename.txt 
    

### AWS Credentials
[AWS Credentials](http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html#using-the-default-credential-provider-chain) 
can be located in three different places. The program will look for credentials in order:

1. Environment variables
2. Java system properties
3. Default credentials profile file
