variable "aws_region" {
  description = "AWS region for the reference deployment."
  type        = string
  default     = "eu-north-1"
}

variable "environment" {
  description = "Short environment name."
  type        = string
  default     = "portfolio"
}

variable "database_username" {
  description = "RDS application user. Supply through TF_VAR_database_username."
  type        = string
  sensitive   = true
}

variable "database_password" {
  description = "RDS password. Supply through TF_VAR_database_password."
  type        = string
  sensitive   = true
}

