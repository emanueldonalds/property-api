# Property listings API

Available at https://api.edonalds.com/listings

This API collects currently listed properties for sale on Åland, with links to the original source.

##  Run

### Using in-memory db:
`./gradlew run --args="--spring.profiles.active=in-mem"`

### Using MariaDb
Set the required env variables:

- `PROPERTY_API_DB_HOST`
- `PROPERTY_API_DB_PORT`
- `PROPERTY_API_DB_PASSWORD`
- `PROPERTY_API_APIKEY`

`./gradlew run`
