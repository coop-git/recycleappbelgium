# RecycleappBelgium Binding

Waste collections in Belgium are shown in an app but also on a website: https://www.recycleapp.be/home
This binding gets the next waste collections for your address.

## Thing Configuration

| Parameter       | type    | description                                          | Required |
|-----------------|---------|------------------------------------------------------|----------| 
| Zip             | String  | Your postalcode (4 digits)                           | true     |
| Street          | String  | Your Street                                          | true     |
| HouseNumber     | String  | Your housenumber                                     | true     |
| refreshInterval | Integer | The refresh interval in HOURS (default=12)           | true     |
| Language        | String  | In what language do you want to see the collections  | false    |

## Channels

| channel  | type   | description                                     |
|----------|--------|-------------------------------------------------|
| NextCollection    | String | Readonly - Date + type of collections  |

## Full Example

Thing recycleappbelgium:Collection:home "Waste@Home" [Zip="1234", Street="OpenHAB street", HouseNumber="1"]
