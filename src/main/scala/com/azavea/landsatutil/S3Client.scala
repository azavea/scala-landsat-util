package com.azavea.landsatutil

import com.amazonaws.services.s3._
import com.amazonaws.auth._
import com.amazonaws.services.s3.model._
import com.amazonaws.retry.PredefinedRetryPolicies

object S3Client {
  private def defaultConfiguration = {
    val config = new com.amazonaws.ClientConfiguration
    config.setMaxConnections(128)
    config.setMaxErrorRetry(16)
    config.setConnectionTimeout(100000)
    config.setSocketTimeout(100000)
    config.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(32))
    config
  }

  def apply(): S3Client =
    new S3Client(new AmazonS3Client(new DefaultAWSCredentialsProviderChain(), defaultConfiguration))
}

class S3Client(val awsClient: AmazonS3Client) {
  def imageExists(image: LandsatImage): Boolean =
    exists("landsat-pds", s"${image.baseS3Path}/")

  def exists(bucket: String, key: String): Boolean = {
    val res = awsClient.listObjects(new ListObjectsRequest(bucket, key, null, null, null))
    import scala.collection.JavaConverters._
    !(res.getObjectSummaries().isEmpty)
  }
}
