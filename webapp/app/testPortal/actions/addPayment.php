<?php

/**
 * Provides access to food history updating. This is only UI interface thing, so there is no JSON :-/
 *
 * Request user, quantity, description, date (of entry, in format from <input type="date>) and price as GET param.
 */


include_once "../libs/foodOrderDbConnector.php";

// Create mysqli connection
$dbConn = connectDbFoodOrder();

// Insert of new history entry
$stmt = $dbConn->prepare("INSERT INTO FoodOrder (FODate, FOFoodID, FOType, FOReserved, FOTaken, FOPrice, FODescription, FOUserID) VALUES(?,?,?,?,?,?,?,?)");

// Prepare variables;
$date = strtotime($_GET["date"] . " UCT");
$foodId = NULL;
$orderType = "payment";

$stmt->bind_param('iisiiisi', $date, $foodId, $orderType, $_GET["quantity"], $_GET["quantity"], $_GET["price"], $_GET["description"], $_GET["user"]);
$stmt->execute();

$dbConn->close();

// Move to history page (where user can the result)
header("Location: ../history.php?user=" . $_GET["user"] . "&format=text");
