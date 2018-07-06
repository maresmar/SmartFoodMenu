<?php

/**
 * Provides tools and methods for FoodOrders database
 */

include_once "foodOrderDbConnector.php";

/**
 * Delete food entry from database
 * @param $db mysqli database
 * @param $foodId int id of food
 * @param $userId int id of user
 * @return bool if operation succeed
 */
function deleteFoodOrder(mysqli $db, int $foodId, int $userId): bool
{
    $stmt = $db->prepare("DELETE FROM FoodOrder WHERE FOFoodID=? AND FOType='standard' AND FOUserID=?");
    $stmt->bind_param('ii', $foodId, $userId);
    return $stmt->execute();
}

/**
 * Insert new food order into database, if there is any previous order it removes the old one and
 * insert the new one.
 * @param $db mysqli database
 * @param $foodId int id of food
 * @param $reserved int >= 0 that specify reserved food
 * @param $offered int >= 0 that specify food offered in food stock
 * @param $taken int >= 0 that specify taken food
 * @param $userId int id of user
 * @return bool if operation succeed
 */
function insertOrUpdateFoodOrder(mysqli $db, int $foodId, int $reserved, int $offered, int $taken, int $userId): bool
{
    // if there is any previous order I will delete it
    $ret = deleteFoodOrder($db, $foodId, $userId);
    if (!$ret)
        return false;

    if ($offered + $taken > $reserved)
        return false;

    // insert new order
    if ($reserved == 0 && $offered == 0 && $taken == 0) {
        // Don't need to insert anything
        return true;
    } else {
        // Insert of new order
        $food = generateFood($foodId, $userId);
        $now = round(microtime(true));
        $stmt = $db->prepare("INSERT INTO FoodOrder (FODate, FOFoodID, FOType, FOReserved, FOOffered, FOTaken, FOUserID, FOPrice) VALUES(?, ?, 'standard', ?, ?, ?, ?, ?)");
        $stmt->bind_param('iiiiiii', $now, $foodId, $reserved, $offered, $taken, $userId, $food->price);
        return $stmt->execute();
    }
}

/**
 * Finds sum of food available to order in food stock
 *
 * That means food putted to food stock by other users.
 * @param $foodId int id of food
 * @param $userId int id of user
 * @return int that represent sum of order in food stock
 */
function remainingInFoodStock(int $foodId, int $userId = -1): int
{
    $conn = connectDbFoodOrder();

    // Check connection
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }

    // Finds the result
    $stmt = $conn->prepare("SELECT IFNULL(SUM(FOOffered), 0) AS SUM FROM FoodOrder WHERE FOFoodID=? AND FOType='standard' AND FOUserID<>?");
    $stmt->bind_param('ii', $foodId, $userId);

    // Check results
    if (!$stmt->execute()) {
        die("Query failed: " . $conn->error);
    }

    // Handle interface db row -> variable
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $ret = $row["SUM"];

    $conn->close();

    return $ret;
}

/**
 * Finds sum of quantity putted to food stock by selected user
 * @param $foodId int id of food
 * @param $userId int id of user
 * @return int that represent quantity of food in food stock
 */
function offeredQuantity(int $foodId, int $userId): int
{
    $conn = connectDbFoodOrder();

    // Check connection
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }

    // Finds the result
    $stmt = $conn->prepare("SELECT FOOffered FROM FoodOrder WHERE FOFoodID=? AND FOType='standard' AND FOUserID=?");
    $stmt->bind_param('ii', $foodId, $userId);

    // Check results
    if (!$stmt->execute()) {
        die("Query failed: " . $conn->error);
    }

    // Handle interface db row -> variable
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $ret = intval($row["FOOffered"]);

    $conn->close();

    return $ret;
}

/**
 * Finds quantity of ordered food
 * @param $foodId int id of food
 * @param $userId int id of user
 * @return int that represent quantity of ordered food
 */
function reservedQuantity(int $foodId, int $userId): int
{
    $conn = connectDbFoodOrder();

    // Check connection
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }

    $stmt = $conn->prepare("SELECT FOReserved FROM FoodOrder WHERE FOFoodID=? AND FOUserID=? AND FOType='standard'");
    $stmt->bind_param('ii', $foodId, $userId);

    // Check results
    if (!$stmt->execute()) {
        die("Query failed: " . $conn->error);
    }

    $result = $stmt->get_result();
    $row = $result->fetch_assoc();

    $ret = intval($row["FOReserved"]);

    $conn->close();

    return $ret;
}
