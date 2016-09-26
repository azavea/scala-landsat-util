package com.azavea.landsatutil

import com.amazonaws.services.s3._
import com.amazonaws.auth._
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

  @transient lazy val default = new AmazonS3Client(new DefaultAWSCredentialsProviderChain(), defaultConfiguration)
}
