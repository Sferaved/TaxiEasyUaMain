package com.taxi.easy.ua.ui.visicom.visicom_search;

class AddressAndCoordinates {
    String[] address;
    double[] coordinates;

    public AddressAndCoordinates(String[] address, double[] coordinates) {
        this.address = address;
        this.coordinates = coordinates;
    }

    // Override equals() and hashCode() methods to allow the set to correctly handle duplicates
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressAndCoordinates that = (AddressAndCoordinates) o;

        return java.util.Arrays.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(address);
    }
}