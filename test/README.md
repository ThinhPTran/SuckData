## Tests

Make sure to edit the `resources/tao2-config.clj` file to point to your glcalcs and 
databases. Currently this is not setup to use relative pathings for glcalcs
and the DBs so this needs to be done manually.  

The DBs that the tests expect are located under `resources/db`. Place them wherever
works for you but set the test config file

---
Run with `lein test`
