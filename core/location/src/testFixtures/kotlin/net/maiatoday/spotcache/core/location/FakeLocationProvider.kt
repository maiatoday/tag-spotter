package net.maiatoday.spotcache.core.location

class FakeLocationProvider(var locationToReturn: LocationData? = null) : LocationProvider {
    override suspend fun getCurrentLocation(): LocationData? = locationToReturn
}
