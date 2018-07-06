<?php

/**
 * Provides access to the list of orders in JSON and in UI
 *
 * Request user as GET param.
 */

include_once "libs/foodGenerator.php";
include_once "libs/foodOrderDbConnector.php";
include_once "libs/foodOrderTools.php";

// Create mysqli connection
$conn = connectDbFoodOrder();

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Actual date rounded to days
$time = ((int)(time() / (60 * 60 * 24))) * (60 * 60 * 24);
$orders_arr = array();

// Find actual orders of future food in database
$stmt = $conn->prepare("SELECT * FROM FoodOrder WHERE FOFoodID IS NOT NULL AND FOUserID=? AND (( FOFoodID / 100 ) >= ?) ORDER BY FOFoodID ASC ");
$stmt->bind_param('ii', $_GET["user"], $time);
$stmt->execute();
$result = $stmt->get_result();

while ($row = $result->fetch_assoc()) {
    // Rename columns to corresponding names in JSON
    $row_array['_ID'] = intval($row['FOID']);
    $row_array['Date'] = intval($row['FODate']) * 1000;
    $row_array['FoodId'] = intval($row['FOFoodID']);
    $row_array['Type'] = $row['FOType'];
    $row_array['Reserved'] = intval($row['FOReserved']);
    $row_array['Offered'] = intval($row['FOOffered']);
    $row_array['Taken'] = intval($row['FOTaken']);
    $row_array['Description'] = $row['FODescription'];
    $row_array['Price'] = intval($row['FOPrice']);

    array_push($orders_arr, $row_array);
}

$conn->close();

// Provides user interface with data (if needed)
if ($_GET["format"] == "text") {
    ?>
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>
            sfm - testPortal - Orders
        </title>
        <link rel="stylesheet" type="text/css" href="res/style.css"/>
    </head>
    <body>
    <h1>
        Orders
    </h1>
    <?php include "libs/navBar.php" ?>
    <table>
        <tr>
            <th> Date of order</th>
            <th> Type of entry</th>
            <th> Price</th>
            <th> Quantity (reserved/offered/taken)</th>
            <th> Food name</th>
            <th> Food date</th>
            <th> User actions</th>
            <th> Dev actions</th>
        </tr>
        <?php
        foreach ($orders_arr as $order) {
            $foodEntry = generateFood($order['FoodId'], $_GET['user']);
            echo "<tr>";
            echo "<td> " . date("Y-m-d H:m", $order['Date'] / 1000) . " </td>";
            echo "<td>" . $order['Type'] . "</td>";
            echo "<td>" . $order['Price'] . "</td>";
            echo "<td>" . $order['Reserved'] . "/" . $order['Offered'] . "/" . $order['Taken'] . "</td>";
            echo "<td> " . $foodEntry->foodName . " </td>";
            echo "<td> " . date("Y-m-d ", $foodEntry->date / 1000) . " </td>";
            echo "<td>";
            // Food can be order if the portal allows it or if there is some food in food stock
            if ($foodEntry->features->orders ||
                ($foodEntry->features->foodStock && (remainingInFoodStock($foodEntry->internalID) > 0))) {
                echo "<a href=\"actions/change.php?id=$foodEntry->internalID&user=" . $_GET["user"] . "&dev=false\">Order</a> ";
            }
            // Food can be putted in food stock if portal supports it and there is already some order
            if ($foodEntry->features->foodStock && (reservedQuantity($foodEntry->internalID, $_GET["user"]) > 0)) {
                echo "<a href=\"actions/change.php?id=$foodEntry->internalID&user=" . $_GET["user"] . "&dev=false\">FoodStock</a> ";
            }
            echo "</td>";
            echo "<td>";
            echo "<a href=\"actions/change.php?id=$foodEntry->internalID&user=" . $_GET["user"] . "&dev=true\">Change</a> ";
            echo "</td>";
            echo "</tr>";
        }
        ?>
    </table>
    </body>
    </html>
    <?php
} else {
    // Prints JSON to web page
    echo json_encode($orders_arr);
    echo "\n\n\n<endora>";
}
?>
