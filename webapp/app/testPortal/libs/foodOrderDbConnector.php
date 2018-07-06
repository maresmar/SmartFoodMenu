<?php

/**
 * Provides password and user less access to FoodOrder table
 */

/**
 * Connect to FoodOrder table from `smf` database.
 * @return mysqli connection to table
 */
function connectDbFoodOrder(): mysqli
{
    // Connection constants
    $serverName = "localhost";
    $userName = "sfmportaluser";
    $password = "BKhZUmeR";
    $dbName = "sfm";

    // Create connection
    $conn = new mysqli($serverName, $userName, $password, $dbName);

    // Check connection
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }

    return $conn;
}
