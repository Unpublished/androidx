/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.app.model;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.location.Location;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents a geographical location with a latitude and a longitude.
 *
 * @deprecated use {@link CarLocation} instead.
 */
// TODO(b/177591131): remove after all host references have been removed.
@Deprecated
public final class LatLng {
    @Keep
    private final double mLat;
    @Keep
    private final double mLng;

    /** Returns a new instance of a {@link LatLng}. */
    @NonNull
    public static LatLng create(double latitude, double longitude) {
        return new LatLng(latitude, longitude);
    }

    /**
     * Returns a new instance of a {@link LatLng} with the same latitude and longitude contained in
     * the given {@link Location}.
     *
     * @throws NullPointerException if {@code location} is {@code null}.
     */
    @NonNull
    public static LatLng create(@NonNull Location location) {
        requireNonNull(location);
        return create(location.getLatitude(), location.getLongitude());
    }

    /** Returns the latitude of the location, in degrees. */
    public double getLatitude() {
        return mLat;
    }

    /** Returns the longitude of the location, in degrees. */
    public double getLongitude() {
        return mLng;
    }

    @Override
    public String toString() {
        return "[" + getLatitude() + ", " + getLongitude() + "]";
    }

    @Override
    public int hashCode() {
        return hash(mLat, mLng);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LatLng)) {
            return false;
        }
        LatLng otherLatLng = (LatLng) other;

        return Double.doubleToLongBits(mLat) == Double.doubleToLongBits(otherLatLng.mLat)
                && Double.doubleToLongBits(mLng) == Double.doubleToLongBits(otherLatLng.mLng);
    }

    private LatLng(double lat, double lng) {
        this.mLat = lat;
        this.mLng = lng;
    }

    /** Constructs an empty instance, used by serialization code. */
    private LatLng() {
        this(0, 0);
    }
}
