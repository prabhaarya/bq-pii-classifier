Steps to add/change an InfoType
* Add infotype to dlp inspection job template in terraform 
* Add a mapping entry to variable infoTypeName_policyTagName_map (info type to policy tag name)
e.g. {info_type = "EMAIL_ADDRESS", policy_tag = "email"}
* Apply terraform (will create/update policy tag in taxonomy and bq config view)