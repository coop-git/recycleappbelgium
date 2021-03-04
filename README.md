# RecycleappBelgium Binding

Waste collections in Belgium are shown in an app but also on a website: https://www.recycleapp.be/home
This binding gets the next waste collections for your address in the next 6 days (default value, can be configured), as from tomorrow.
If nothing is to be collected, the channel will not be updated.

## Thing Configuration

| Parameter       | type    | description                                          | Required          |
|-----------------|---------|------------------------------------------------------|-------------------| 
| Zip             | String  | Your postalcode (4 digits)                           | true              |
| Street          | String  | Your Street                                          | true              |
| HouseNumber     | String  | Your housenumber                                     | true              |
| refreshInterval | Integer | The refresh interval in HOURS (default=12)           | true              |
| Language        | String  | In what language do you want to see the collections  | true              |
| daysToCheck     | Integer | How many days in the future to check for Collections | false (default=6) |
| token           | String  | Token obtained from recycleapp website               | true              |

Token can be retrieved via Developer Tools of browser. Otherwise look for the topic _Waste collections Belgium_ on the OpenHAB Community.

## Channels

| channel  | type   | description                                     |
|----------|--------|-------------------------------------------------|
| NextCollection    | String | Readonly - Date + type of collections  |

Format: (YYYY-MM-DD)Collection1***Collection2...
If something goes wrong in the process of getting the data, a message will be shown in the channel instead. This will only be text.

## Full Example

Thing recycleappbelgium:Collection:home "Waste@Home" [Zip="1234", Street="OpenHAB street", HouseNumber="1", token="xyz123"]
