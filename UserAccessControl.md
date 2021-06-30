Service Project
 - facilities

IAM
 - (group) project-facilities-conf-data-readers@
    - (user) facilities-conf-data-reader@
 - (group) project-zbooks-conf-data-readers@
    - (user) zbooks-conf-data-reader@
 - (group) project-facilities-standard-data-readers@
    - (user) facilities-standard-data-reader@
 - (group) project-zbooks-standard-data-readers@
    - (user) zbooks-standard-data-reader@
    
    
Roles & Permissions

- (Project) facilities
     - (group) project-facilities-conf-data-readers@
         - BigQuery Data Viewer --> access BQ data
         - Data Catalog Viewer --> view policy tag name hosted on service project (facilities)
     - (group) project-zbooks-conf-data-readers@
         - Data Catalog Viewer --> view policy tag name hosted on service project (facilities)
     - (group) project-facilities-standard-data-readers@
        - BigQuery Data Viewer --> access BQ data
     - (group) project-zbooks-standard-data-readers@
             - BigQuery Data Viewer --> access BQ data
     
- (Project) zbooks
     - (group) project-zbooks-conf-data-readers@
         - BigQuery Data Viewer --> access BQ data
     - (group) project-zbooks-standard-data-readers@
         - BigQuery Data Viewer --> access BQ data
        
(Taxonomy)
 - project-facilities-conf-data-readers@
    - Fine Grain Reader
 - project-zbooks-conf-data-readers@
        - Fine Grain Reader
        

This setup result in the following access:
project-facilities-conf-data-readers@ = std + conf in facilities
project-zbooks-conf-data-readers@ = std + conf in zbooks
project-facilities-standard-data-readers@ = std in facilities
project-zbooks-standard-data-readers@ = std in facilities and zbooks

However, If we add
- (Project) zbooks
     - (group) project-facilities-conf-data-readers@
         - BigQuery Data Viewer --> access BQ data
         
we get 
project-facilities-conf-data-readers@ = std + conf in facilities and zbooks

[once a conf reader in one project it can only have zero access or conf access to other projects, unless table level security is used to scope down access]
Solutions:
- it's ok for conf reader to access conf data in all projects they have read access to
- deploy the solution per project
- create one taxonomy per project and configure a map of maps for infoType-policytag




