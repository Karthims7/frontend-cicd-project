# Database Schema & Initialization

This directory contains the database initialization scripts for the Kubernetes lab.

## Files
- `init.sql`: Creates the `users` table and populates sample records (`karthi`, `devops_lead`, `test_user`).

## Manual Execution / Verification
To execute or re-seed the PostgreSQL database running in Kubernetes:

```bash
kubectl exec -i $(kubectl get pod -l app=postgres -o jsonpath='{.items[0].metadata.name}') -- psql -U appuser -d appdb < database/init.sql
```

To query the database:
```bash
kubectl exec $(kubectl get pod -l app=postgres -o jsonpath='{.items[0].metadata.name}') -- psql -U appuser -d appdb -c "SELECT * FROM users;"
```
