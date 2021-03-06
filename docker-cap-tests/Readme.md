# CWDS PERRY CAP PERFORMANCE TESTS

This image can be used to run a set of performance tests against PERRY CAP API or PERRY CAP Rails layer.

## Configuration
| Parameter | Description  | Required  | Possible values | Default value | Example |
| :------------ | :------------ | :------------ | :------------ | :------------ | :------------ |
| **JM_PERRY_PROTOCOL** | The protocol for the Perry connection | + | http; https |  | https |
| **JM_PERRY_HOST** | The host for the Perry connection | + | any host |  | web.dev.cwds.io |
| **JM_PERRY_PORT** | The port for the Perry connection | + | Any port like 80 (for http) or 443 (for https) |  | 443 |
| **JM_TARGET**  | The target application to be tested (Api java app or Rails app) | - | api; rails | api | rails |
| **JM_DATA_DIR** | Path to the directory containing input data (such as users csv file). | - | any directory | /opt/cap-tests/perf/data | c:/work/test_data |
| **JM_USERS_CSV_FILENAME** | Name of the csv file containing users names and passwords(in {name}\|{password} format). Should be put in JM_DATA_DIR directory| - | any file name | users.csv | test_users.csv |
| **JM_RESULTS_DIR** | Path to the directory containing test results| - | any directory | /opt/cap-tests/perf/results | c:/work/test_results |
| **JM_USERS_COUNT** | How many simultaneous users will be imitated by tests | - | Any positive integer. If every thread has to be executed with different user then this number should be less or equal the number of different users in users csv file| 1 | 100 |
| **JM_REQUESTS_PER_USER** | How many sequential requests will make each user | - | Any positive integer. | 1 | 100 |
| **JM_RAMP_UP_PERIOD_SEC** | Time (in seconds) in which all threads are started| - | Any positive integer. | 1 | 2 |

JM_DATA_DIR and JM_RESULTS_DIR should not be set when tests are started from Docker container. 
Instead some host directories have to be mapped to the  Docker volumes with names equal to 
default JM_DATA_DIR and JM_RESULTS_DIR values (/opt/cap-perf-tests/data and /opt/cap-perf-tests/results)


# How to to run:
## From Docker container
```
docker run -e "JM_TARGET=api" \
  -e "JM_PERRY_PROTOCOL=https" \
  -e "JM_PERRY_HOST=web.dev.cwds.io" \
  -e "JM_PERRY_PORT=443" \
  -e "JM_USERS_COUNT=50" \
  -e "JM_REQUESTS_PER_USER=100" \
  -e "JM_RAMP_UP_PERIOD_SEC=2" \
  -e "JM_USERS_CSV_FILENAME=users.csv" \
  -v c:/work/cap-tests/data:/opt/cap-perf-tests/data  \
  -v c:/work/cap-tests/results:/opt/cap-perf-tests/results \
  --rm cwds/cap-test
```
### Result
The container entrypoint.sh script returns 0 if tests start successfully and 1 otherwise.

## From JMeter UI
 * Open test .jmx file in JMeter UI.
 * In the root Test Plan element set all variables using their default values or by setting correspondent environment variables.
