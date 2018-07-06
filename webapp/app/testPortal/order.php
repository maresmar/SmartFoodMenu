<?php

/**
 * Provides access to food order changing. The result is printed in JSON and in UI
 * provided as redirection to order list.
 *
 * Request user, id (internal food id), type (type of order), quantity as GET param.
 */

include_once "libs/foodGenerator.php";
include_once "libs/foodOrderDbConnector.php";
include_once "libs/foodOrderTools.php";

/**
 * Class FoodOrderResult is used for putting results into JSON
 */
class FoodOrderResult
{
    public $ok = true;
    public $msg = "";
}

/**
 * This method transfers food from food stock to user's orders.
 * @param mysqli $db connection na smf database
 * @param int $reserved food to be reserved from others
 * @param int taken food already taken
 * @param int $foodId internal id of food
 * @param int $userId id of user
 * @return FoodOrderResult ->ok=true if operation succeed; false otherwise, message contains error info
 */
function increaseOrderedFromFoodStock(mysqli $db, int $reserved, $taken, int $foodId, int $userId): FoodOrderResult
{
    $remaining = $reserved - reservedQuantity($_GET["id"], $_GET["user"]);
    $result = new FoodOrderResult();

    // Find food in food stock
    $stmt = $db->prepare("SELECT * FROM FoodOrder WHERE FOFoodID=? AND FOOffered>0 AND FOType='standard'");
    $stmt->bind_param('i', $_GET["id"]);
    $stmt->execute();
    $dbResult = $stmt->get_result();
    // Transform food from food stock to regular order
    while (($row = $dbResult->fetch_assoc()) && $remaining > 0) {
        // For each food in food stock
        if ($remaining >= $row["FOOffered"]) {
            $offered = $row["FOOffered"];
        } else {
            $offered = $remaining;
        }
        insertOrUpdateFoodOrder($db, $row["FOFoodID"], $row["FOReserved"] - $offered,
            $row["FOOffered"] - $offered, $row["FOTaken"], $row["FOUserID"]);

        $remaining -= $offered;
    }

    // Check results
    if ($remaining > 0) {
        // I did't fond enough food in food stock
        $result->ok = false;
        $result->msg .= "Insufficient food in food stock (cannot found last $remaining) ";
    }

    // Save transformed order
    $ret = insertOrUpdateFoodOrder($db, $foodId, $reserved - $remaining, 0,
        $taken, $userId);
    if (!$ret) {
        $result->ok = false;
        $result->msg = "Error when updating food order ";
    }

    return $result;
}

// Main code block

// Create mysqli connection
$dbConn = connectDbFoodOrder();

// Create result object
$orderResult = new FoodOrderResult();
$orderResult->ok = true;
// Helping variable
$res = true;
$menuEntry = generateFood($_GET["id"], $_GET["user"]);

if ($_GET["offered"] > $_GET["reserved"]) {
    $orderResult->ok = false;
    $orderResult->msg .= "Insufficient food (assert failed: offered <= reserved) ";
}

if ($_GET["offered"] + $_GET["taken"] > $_GET["reserved"]) {
    $orderResult->ok = false;
    $orderResult->msg .= "Insufficient food (assert failed: offered + taken <= reserved) ";
}

// Handle order
if ($orderResult->ok) {
    $oldReserved = reservedQuantity($_GET["id"], $_GET["user"]);
    $newReserved = $_GET["reserved"];
    if ($menuEntry->features->orders || $oldReserved == $newReserved || $_GET["dev"] == "true") {
        // Change of order
        if ($_GET["reserved"] == 0) {
            // Delete row of FoodOrder (if exists)
            $res = deleteFoodOrder($dbConn, $_GET["id"], $_GET["user"]);
            $orderResult->ok = $orderResult->ok && $res;
        } else {
            // Update or insert new FoodOrder
            // Delete other entries from the same group
            $groupId = floor($_GET["id"] / 10);
            $stmt = $dbConn->prepare("DELETE FROM FoodOrder WHERE FOUserID=? AND FOFoodID DIV 10=?");
            $stmt->bind_param('ii', $_GET["user"], $groupId);
            $orderResult->ok = $stmt->execute();
            // Let's make new order
            $res = insertOrUpdateFoodOrder($dbConn, $_GET["id"], $_GET["reserved"], $_GET["offered"], $_GET["taken"], $_GET["user"]);
            $orderResult->ok = $orderResult->ok && $res;
        }
    } else {
        // Reserve food from food stock
        if ($oldReserved > $newReserved) {
            $orderResult->ok = false;
            $orderResult->msg .= "Cannot lower reserved amount if entry is disabled, you can offer something";
        } else {
            if ($_GET["offered"] > 0) {
                $orderResult->ok = false;
                $orderResult->msg .= "Doesn't make sense order sth from food stock if you already offer same food in it";
            }
            $orderResult = increaseOrderedFromFoodStock($dbConn, $newReserved, $_GET["taken"], $_GET["id"], $_GET["user"]);
        }
    }
}

$dbConn->close();

if ($_GET["format"] == "text") {
    // UI mode
    if ($orderResult->ok)
        // Redirect to the list of orders
        header("Location: orders.php?user=" . $_GET["user"] . "&format=text");
    else
        echo "Error in inserting order to database:\n$orderResult->msg";
} else {
    // Prints JSON to web page
    echo json_encode($orderResult);
    echo "\n\n\n<endora>";
}