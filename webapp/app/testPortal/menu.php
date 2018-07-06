<?php

/**
 * Provides access to menu in JSON and in UI
 *
 * Request user as GET param.
 */

include_once "libs/foodGenerator.php";
include_once "libs/foodOrderTools.php";

// Results
$menuEntries = array();

// Generates list of food Ids (The food is generated from food Ids)
// Actual date rounded to days
$time = ((int)(time() / (60 * 60 * 24))) * (60 * 60 * 24);
// Menu will be for next 10 days
for ($i = 0; $i < 10; $i++) {
    $dayIndex = 0;
    $groupIndex = 0;
    // Each day has 3 types of food
    for ($groupIndex = 1; $groupIndex <= 3; $groupIndex++) {
        // Each type has three variants of food
        for ($groupVariantIndex = 1; $groupVariantIndex <= 3; $groupVariantIndex++) {
            $menuEntries[] = generateFood("$time$groupIndex$groupVariantIndex", $_GET["user"]);
        }
    }
    // Move to next day
    $time = $time + 60 * 60 * 24;
}

// Provides user interface with data (if needed)
if ($_GET["format"] == "text") {
    ?>
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>
            sfm - testPortal - Menu
        </title>
        <link rel="stylesheet" type="text/css" href="res/style.css"/>
    </head>
    <body>
    <h1>
        Menu
    </h1>
    <?php include "libs/navBar.php" ?>
    <table>
        <tr>
            <th> Date</th>
            <th> Name of food</th>
            <th> Food group</th>
            <th> Text</th>
            <th> Price</th>
            <th> Labels</th>
            <th> Reserved</th>
            <th> In food stock <br/> (my/global)</th>
            <th> User actions</th>
            <th> Dev actions</th>
        </tr>
        <?php
        // Prints one food entry
        foreach ($menuEntries as $menuEntry) {
            echo "<tr>";
            echo "<td> " . date("Y-m-d ", $menuEntry->date / 1000) . " </td>";
            echo "<td> $menuEntry->foodName </td>";
            echo "<td> $menuEntry->foodGroup </td>";
            echo "<td> $menuEntry->foodText </td>";
            echo "<td> $menuEntry->price </td>";
            echo "<td> $menuEntry->labels </td>";
            echo "<td> " . reservedQuantity($menuEntry->internalID, $_GET["user"]) . " </td>";
            echo "<td> " . offeredQuantity($menuEntry->internalID, $_GET["user"]) . "/"
                . remainingInFoodStock($menuEntry->internalID) . " </td>";
            echo "<td>";
            // Food can be order if the portal allows it or if there is some food in food stock
            if ($menuEntry->features->orders || ($menuEntry->features->foodStock &&
                    (remainingInFoodStock($menuEntry->internalID) > 0))) {
                echo "<a href=\"actions/change.php?id=$menuEntry->internalID&user=" . $_GET["user"] . "&dev=false\">Order</a> ";
            }
            // Food can be putted in food stock if portal supports it and there is already some order
            if ($menuEntry->features->foodStock && (reservedQuantity($menuEntry->internalID, $_GET["user"]) > 0)) {
                echo "<a href=\"actions/change.php?id=$menuEntry->internalID&user=" . $_GET["user"] . "&dev=false\">FoodStock</a> ";
            }
            echo "</td>";
            echo "<td>";
            echo "<a href=\"actions/change.php?id=$menuEntry->internalID&user=" . $_GET["user"] . "&dev=true\">Change</a> ";
            echo "</td>";
            echo "</tr>";
        }
        ?>
    </table>
    </body>
    </html>
    <?php
} else {
    $data = array();

    // Binds rows from menu to array that can be printed in JSON
    foreach ($menuEntries as $menuEntry) {
        // Rename columns to corresponding names in JSON
        $row_array['relativeId'] = $menuEntry->internalID;
        $row_array['text'] = $menuEntry->foodText;
        $row_array['group'] = $menuEntry->foodGroup;
        $row_array['label'] = $menuEntry->foodName;
        $row_array['date'] = $menuEntry->date;
        // $row_array['remainingToTake'] = ... not used needed
        // $row_array['remainingToOrder'] = ... not used needed
        // $row_array['extra'] = ... not needed
        $row_array['price'] = $menuEntry->price;

        $features = 0;
        if ($menuEntry->features->orders) {
            $features |= 3;
        }
        if ($menuEntry->features->foodStock) {
            $features |= 4;
        }
        $row_array['features'] = $features;
        $row_array['remainingToOrder'] = -1;
        $row_array['remainingToTake'] = -1;

        array_push($data, $row_array);
    }

    // Prints JSON (in standard case without UI)
    echo json_encode($data);
    echo "\n\n\n<endora>";
}
?>
