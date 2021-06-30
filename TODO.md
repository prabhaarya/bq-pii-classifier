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
- [ ] Discuss multi-project confedential data readers with VM and agree on solution
- [ ] Code cleanup/refactoring/unit tests perspective

# Logging & Reporting
- [x] apply structured logging to all functions
- [x] logging table and views in BQ for tracking broken chains
- [x] logging view for actions on tags

# Testing
- [ ] test cross-projects permissions (including column level security on target project)
- [ ] Add unit tests
- [ ] Test terraform inspection template with custom info types
- [ ] Test on more projects, tables, data (stress test)


# Infra automation
- [x] Terraform everything
- [ ] Queues config

# Improvements should-do
- [ ] mapping infotypes to taxonomies in terraform
- [x] check if multi-location is required and implement it (not required now)
- [ ] If needed for performance. Use projectStarter function to trigger dispatcher with one project only
 
# Improvements nice-to-have
- [ ] Monitoring dashboard and alerts


