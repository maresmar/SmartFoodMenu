<?php

/**
 * Provides access to history in JSON and in UI
 *
 * Request user as GET param.
 */

include_once "libs/foodGenerator.php";
include_once "libs/foodOrderDbConnector.php";

// Create mysqli connection
$dbConn = connectDbFoodOrder();

// Check connection
if ($dbConn->connect_error) {
    die("Connection failed: " . $dbConn->connect_error);
}

$history_arr = array();
// Actual date rounded to days
$time = ((int)(time() / (60 * 60 * 24))) * (60 * 60 * 24) * 1000;

// Find history entries in database
$stmt = $dbConn->prepare("SELECT * FROM FoodOrder WHERE FOUserID=? ORDER BY FOFoodID ASC");
$stmt->bind_param('i', $_GET["user"]);
$stmt->execute();
$result = $stmt->get_result();

// Handles results
while ($row = $result->fetch_assoc()) {
    // Only past orders belongs to history or special 'history' entries
    $isHistoryEntry = false;
    if ($row['FOFoodID'] != NULL) {
        // Regular order entry
        $foodEntry = generateFood(intval($row['FOFoodID']), $_GET["user"]);
        if ($foodEntry->date <= $time) {
            $isHistoryEntry = true;
        } else
            $isHistoryEntry = false;
    } else {
        // Special 'history' entry
        $isHistoryEntry = true;
    }

    if ($isHistoryEntry) {
        // Rename columns to corresponding names in JSON
        $row_array['_ID'] = intval($row['FOID']);
        $row_array['Date'] = intval($row['FODate']) * 1000;
        $row_array['FoodId'] = $row['FOFoodID'];
        $row_array['Type'] = $row['FOType'];
        $row_array['Reserved'] = intval($row['FOReserved']);
        $row_array['Offered'] = intval($row['FOOffered']);
        $row_array['Taken'] = intval($row['FOTaken']);
        $row_array['Description'] = $row['FODescription'];
        $row_array['Price'] = intval($row['FOPrice']);

        array_push($history_arr, $row_array);
    }
}

$dbConn->close();

if ($_GET["format"] == "text") {
    ?>
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>
            sfm - testPortal - History
        </title>
        <link rel="stylesheet" type="text/css" href="res/style.css"/>
    </head>
    <body>
    <h1>
        History
    </h1>
    <?php include "libs/navBar.php" ?>
    <table>
        <tr>
            <th> Date</th>
            <th> Type of entry</th>
            <th> Description</th>
            <th> Price</th>
            <th> Reserved</th>
            <th> Offered</th>
            <th> Taken</th>
            <th> FoodID</th>
        </tr>
        <?php
        foreach ($history_arr as $order) {
            echo "<tr>";
            echo "<td> " . date("Y-m-d H:m", $order['Date'] / 1000) . " </td>";
            echo "<td>" . $order['Type'] . "</td>";
            echo "<td>" . $order['Description'] . "</td>";
            echo "<td>" . $order['Price'] . "</td>";
            echo "<td>" . $order['Reserved'] . "</td>";
            echo "<td>" . $order['Offered'] . "</td>";
            echo "<td>" . $order['Taken'] . "</td>";
            echo "<td> " . $order['FoodId'] . " </td>";
            echo "</tr>";
        }
        ?>
    </table>
    <a href="actions/payment.php?user=<?php echo $_GET['user']; ?>">Add new payment entry</a>
    </body>
    </html>
    <?php
} else {
    // Prints JSON (in standard case without UI)
    echo json_encode($history_arr);
    echo "\n\n\n<endora>";
}
?>
