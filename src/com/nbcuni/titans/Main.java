package com.nbcuni.titans;

import java.io.File;
import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.commons.cli.*;


public class Main {

    public static void main(String[] args) {
        String existingBucketName = "";
        String keyName = "";
        String filePath = "";
        String proxyHost = null;
        int proxyPort = 0;
        int threads = 10;  // Default AWS SDK value

        CommandLineParser parser = new DefaultParser();

        Option optBucket = new Option("bucket", "Target S3 Bucket");
        optBucket.setArgs(1);
        optBucket.setRequired(true);
        Option optKey = new Option("key", "Target S3 filename or key");
        optKey.setArgs(1);
        Option optFile = new Option("file", "Source file to be uploaded");
        optFile.setArgs(1);
        optFile.setRequired(true);
        Option optThreads = new Option("threads", "Number of upload threads");
        optThreads.setArgs(1);
        Option optProxyHost = new Option("proxyHost", "HTTP Proxy host. Must be used with port (e.g., 173.213.212.248)");
        optProxyHost.setArgs(1);
        Option optProxyPort = new Option("proxyPort", "HTTP Proxy Port. Must be used with host (e.g., 80)");
        optProxyPort.setArgs(1);

        Options options = new Options();
        options.addOption(optBucket);
        options.addOption(optKey);
        options.addOption(optFile);
        options.addOption(optThreads);
        options.addOption(optProxyHost);
        options.addOption(optProxyPort);
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("S3Copy", options);

        try {
            CommandLine line = parser.parse(options,args);
            if(!line.hasOption("key")) {
                keyName = line.getOptionValue("file");
            }
            else {
                keyName = line.getOptionValue("key");
            }
            if(line.hasOption("threads")) {
                threads = Integer.parseInt(line.getOptionValue("threads"));
            }
            if(line.hasOption("proxyPort")) {
                proxyPort = Integer.parseInt(line.getOptionValue("proxyPort"));
            }
            if(line.hasOption("proxyHost")) {
                proxyHost = line.getOptionValue("proxyHost");
            }
            existingBucketName = line.getOptionValue("bucket");
            filePath = line.getOptionValue("file");
        }
        catch (ParseException ex) {
            System.out.println("Unexpected exception: " + ex.getMessage());
            System.exit(1);
        }

        /*
         * http://stackoverflow.com/questions/23912407/frequent-nohttpresponseexception-with-amazons3-getobjectrequest-getobjectconte
         */
        ClientConfiguration clientConfiguration;
        if (proxyHost == null || proxyPort == 0) {
            clientConfiguration = new ClientConfiguration()
                    .withMaxConnections(threads)
                    .withMaxErrorRetry(10)
                    .withConnectionTimeout(10_000)
                    .withSocketTimeout(10_000)
                    .withTcpKeepAlive(true);
        }
        else {
            clientConfiguration = new ClientConfiguration()
                    .withProxyHost(proxyHost)
                    .withProxyPort(proxyPort)
                    .withMaxConnections(threads)
                    .withMaxErrorRetry(10)
                    .withConnectionTimeout(10_000)
                    .withSocketTimeout(10_000)
                    .withTcpKeepAlive(true);
        }

        System.out.println("Max http connections: " + clientConfiguration.getMaxConnections());

        /*
         * http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html#using-the-default-credential-provider-chain
         * Default credentials provider will look in:
         * 1. Environment variables
         * 2. Java system properties
         * 3. Default credentials profile file
         */
        TransferManager tm = new TransferManager(
                new AmazonS3Client(new DefaultAWSCredentialsProviderChain(), clientConfiguration));
        TransferManagerConfiguration txManagerConfig = new TransferManagerConfiguration();
        System.out.println("S3 Transfer Manager Initialized");
        System.out.println("Min Upload Part Size: " + txManagerConfig.getMinimumUploadPartSize());
        System.out.println("Min Multipart Upload Threshold: " + txManagerConfig.getMultipartUploadThreshold());

        // TransferManager processes all transfers asynchronously,
        // so this call will return immediately.
        Upload upload = tm.upload(
                existingBucketName, keyName, new File(filePath));
        System.out.println("Uploading Started");


        try {
            // Print upload status during process
            System.out.println("Transfer: " + upload.getDescription());
            //System.out.println("  - State: " + upload.getState());
            while (upload.isDone() == false) {

                System.out.print("..."
                        + Math.round(upload.getProgress().getPercentTransferred()) + "% ");
                Thread.sleep(3000);
            }


            // Or block and wait for the upload to finish
            //upload.waitForCompletion();

            System.out.println("Upload Complete.");

        } catch (AmazonClientException amazonClientException) {
            System.out.println("Unable to upload file, upload was aborted.");
            amazonClientException.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("Upload interrupted: " + e.getStackTrace());
            System.exit(1);
        } finally {
            tm.shutdownNow();
        }
    }
}
