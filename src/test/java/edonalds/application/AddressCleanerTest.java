package edonalds.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AddressCleanerTest {

    @Test
    void testCleanAddresses() {
        assertEquals("", AddressCleaner.clean(""));
        assertEquals("", AddressCleaner.clean("     "));
        assertEquals("", AddressCleaner.clean(null));
        assertEquals("Ålandsvägen 23 Åland", AddressCleaner.clean("Ålandsvägen 23"));
        assertEquals("3 Ängsvägen Listersby Åland", AddressCleaner.clean("3 Ängsvägen, Listersby"));
        assertEquals("Ålandsvägen 30 Storängen Åland", AddressCleaner.clean("Ålandsvägen 30B, Storängen"));
        assertEquals("Askuddsvägen 1 Åland", AddressCleaner.clean("Askuddsvägen 1 D 22"));
        assertEquals("Bjärströmsgatan 7 Åland", AddressCleaner.clean("Bjärströmsgatan 7A"));
        assertEquals("Ålandsvägen 32 Mariehamn Åland", AddressCleaner.clean("Ålandsvägen 32 lokal 102, Mariehamn"));
        assertEquals("Ålandsvägen 35 Mariehamn Åland", AddressCleaner.clean("Ålandsvägen 35 Bst 8, AX-22100 Mariehamn"));
        assertEquals("Årsringen 7 Åland", AddressCleaner.clean("Årsringen 7 i Mariehamn"));
        assertEquals("Bastövägen 531 Finström Åland", AddressCleaner.clean("Bastövägen 531 stuga nr. 22, Finström"));
        assertEquals("Bolvägen Åland", AddressCleaner.clean("Bolvägen/Bolgränd, Finström"));
        assertEquals("Bostadsområdet Åland", AddressCleaner.clean("Bostadsområdet i Tosarby"));
        assertEquals("Flakavägen 496 Lemland Åland", AddressCleaner.clean("Flakavägen 496, AX-22610 Lemland."));
        assertEquals("Skaguddsvägen Åland", AddressCleaner.clean("Skaguddsvägen kvarter 2 tomt 3"));
        assertEquals("", AddressCleaner.clean("Del av holmen Brändholm, Kumlinge"));
        assertEquals("Prästgårdsvägen 5&7 Kapellvägen 2 Lumparland Åland", AddressCleaner.clean("Prästgårdsvägen 5&7, Kapellvägen 2, Lumparland"));
        assertEquals("Årsringen 7 Åland", AddressCleaner.clean("Årsringen 7 i Mariehamn, Åland"));
    }
}
