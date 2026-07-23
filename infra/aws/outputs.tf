output "eks_cluster_name" {
  value = aws_eks_cluster.main.name
}

output "postgres_endpoint" {
  value     = aws_db_instance.positions.endpoint
  sensitive = true
}

output "redis_primary_endpoint" {
  value     = aws_elasticache_replication_group.risk.primary_endpoint_address
  sensitive = true
}

output "kafka_bootstrap_brokers" {
  value     = aws_msk_serverless_cluster.events.bootstrap_brokers_sasl_iam
  sensitive = true
}

