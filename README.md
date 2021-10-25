# Data Warehouse

## Object Model

### Measurements
The core entity, it represents a measurement recorded, where a measurement can have one or more values (a field) where 
each value is for a specific metric. 

### Fields
Single recorded value for a specific metric

### Metrics
There are two types of metrics:
- Measured  
- Virtual

#### Virtual Metrics
Virtual metrics are calculated as a formula applied to one or more measured metrics. The formula aspect is currently 
quite rudimentary, and is something that could be developed over time (perhaps a domain specific language) to make it
adequately robust to allow users not familiar with the inner workings of the system to be able to write them.

Virtual metrics can be created with the api. See the following example:

```
POST /virtualMetrics
```
```JSON
{
  "name": "Ctr",
  "formula": "Clicks / Impressions",
  "requiredMetrics": [1 , 2]
}
```

#### Measured Metrics
Measured metrics are concrete values, backed by real data stored in the database. A measured metric can be created 
using the api, but measured metrics are also automatically created as part of the loading process.

### Dimensions and Dimension Values
Allows measurements to be tagged with additional categories of data, that can then be grouped on.

## Queries

### Example Queries
- /query?metrics=Clicks&rangeStart=2019-11-14&rangeEnd=2019-11-16&groupBy=Datasource&filterBy=Datasource:Google Ads
- /query?metrics=Ctr&rangeStart=2019-11-01&groupBy=Datasource,Campaign
- /query?metrics=Impressions&rangeStart=2019-01-01
- /query?metrics=Impressions,Clicks,Ctr&rangeStart=2019-01-01

### Parameter Validation
Parameters are validated against the database to ensure only valid values are accepted and subsequent parameter 
substitution in the JPQL query ensures protection against SQL injection.

### Parameters
- metric (required)
- rangeStart - start of date range. Format yyyy-MM-dd. Defaults to 7 days ago.
- rangeEnd - end of date range. Format yyyy-MM-dd. Defaults to today.
- filterBy - one or more key:value pairs to filter results by
- groupBy - one or more dimensions to group results by. Defaults to grouping by date.

### Query Result
As the structure of the query results depends on the query made, it's structure is flexible (and also conveniently compact)

#### Columns
A list of column names that correspond to the adjacent data. The columns correspond to the metrics requested
followed by any applicable groups. 

#### Data
A list of result data where each item where each item on the list contains the aggregated data of the 
metrics requested and the corresponding dimension/group values.

#### Example Request and Result
```
GET /query?metrics=Clicks,Impressions&groupBy=Campaign,Datasource
```
```JSON
{
    "columns": ["Clicks", "Impressions", "Campaign", "Datasource"],
    "data": [
      [ 2, 10, "Christmas", "Google"],
      [ 4, 10, "Easter", "Google"]
    ]
}
```

## Loading new measurements

### Content Types
The following content types are currently supported:
- text/csv

### Parameters
- metrics - A comma delimited list of metrics
- dimensions - A comma delimited list of dimensions
- dateName - the attribute name for the date
- dateFormat - the format the date is in

### Example Requests
```
POST /load?metrics=Clicks,Impressions&dimensions=Campaign,Datasource&dateName=Date&dateFormat=yyyy-MM-dd

Date,Clicks,Impressions,Campaigns,Datasource
2011-10-23,20,40,Christmas,Google
2011-10-23,1,1564,Easter,Amazon
2011-10-22,1,1564,Easter,Amazon
```

```
POST /load?metrics=Clicks,Impressions&dimensions=Campaign,Datasource&dateName=Daily&dateFormat=MM/dd/yy

Datasource,Campaign,Daily,Clicks,Impressions
Google Ads,Adventmarkt Touristik,11/12/19,7,22425
Google Ads,Adventmarkt Touristik,11/13/19,16,45452
Google Ads,Adventmarkt Touristik,11/14/19,147,80351
Google Ads,Adventmarkt Touristik,11/15/19,131,81906
```

## Security

### Authentication
Initial authentication is achieved by sending a POST request with a correct username and password to the `/login` URI.
If successful, a JWT token is returned which is then used to make subsequent requests. 

### Authorisation
A user can have one of 3 roles
- ANALYST - Can only read/analyse the data
- COLLECTOR - The privileges of the ANALYST but can also load data into the system
- ADMIN - The privileges of the COLLECTOR, and can also create and delete users.  

### First User
A first user will be created when the app starts. This values can be defined as system properties on 
the command line or as environment variables. Appropriate environment variables are expected in production, 
but not required for test and development.
The properties are the following:
- DATA_WAREHOUSE_USER
- DATA_WAREHOUSE_PASSWORD