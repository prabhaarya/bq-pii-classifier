Every section has its own priority list


# Solution
- [x] Cloud Scheduler to trigger dispatcher
- [x] Bigquery results table + deployment
- [x] handle failed DLP jobs in Tagger (log error and fail)
- [x] Test tagging on parent or child (possible)
- [x] overwrite main taxonomy only
- [x] test group access on multiple projects --> results in UserAccessControl.md
- [x] Use terraform for inspection template as per https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/data_loss_prevention_inspect_template
- [x] move include lists to params and use in scheduler 
- [x] Simplify tagger by creating an InfoType to taxonomy mapping in a BQ view (terraform)
- [x] Change query to select one policy tag per field (in case multiple infotypes found)
- [x] refactor all functions in one module
- [x] Code cleanup/refactoring/unit tests perspective
- [x] enable cross-projects permissions (including column level security on target project)
- [x] implement Multi-project heirarchy/taxonomy to control conf access on project level and org level
- [x] Auto generate policy tags for projects
- [x] auto generate policy tags mapping view for projects
- [x] Configure info types and taxonomy to scan VM fake data
- [ ] Demo to VM

# Logging & Reporting
- [x] apply structured logging to all functions
- [x] logging table and views in BQ for tracking broken chains
- [x] logging view for actions on tags

# Testing
- [x] Add unit tests
- [x] Test terraform inspection template with custom info types
- [ ] Test on more projects, tables, data (stress test)


# Infra automation
- [x] Terraform everything
- [ ] Queues config

# Improvements should-do
- [x] check if multi-location is required and implement it (not required now)
 
# Improvements nice-to-have
- [ ] Monitoring dashboard and alerts

# Docs
- [x] updated diagram
- [] READ ME
- [] blog post


