<?php

/**
 * Provides methods that generates information about food from food id. It also includes
 * model classes for data storing and JSON sharing.
 */

/**
 * Class SupportedFeatures tells witch features can be done on specific food.
 *
 * See docs for extra info. It is also used in JSON.
 */
class SupportedFeatures
{
    public $orders;
    public $foodStock;
    public $remainingFood;
}

/**
 * Class MenuEntry describes specific food.
 */
class MenuEntry
{
    public $internalID;
    public $foodName;
    public $foodGroup;
    public $foodText;
    public $price;
    /**
     * @var integer that represents data in Java format (milliseconds from 1.1.1970) = time() * 1000
     */
    public $date;
    public $labels;
    public $remainingToTake;
    public $remainingToOrder;
    /**
     * @var SupportedFeatures reference
     */
    public $features;
}

/**
 * Generates food information about food from food ID
 * @param $internalID integer in following format : "$time$groupIndex$groupVariantIndex", where
 * $time is standard PHP time (in seconds), $groupIndex and $groupVariantIndex are integers <1,3>
 * @param $user integer id of user
 * @return MenuEntry generated menu info
 */
function generateFood(int $internalID, int $user): MenuEntry
{
    $info = $internalID;

    // Actual date rounded to days
    $today = ((int)(time() / (60 * 60 * 24))) * (60 * 60 * 24);

    // Parse info about food from ID
    $info = intval($info);
    $groupVariantIndex = $info % 10;
    $info = floor($info / 10);
    $groupIndex = $info % 10;
    $date = intval(floor($info / 10));

    // Create food entry
    $groupName = array("Breakfast", "Lunch", "Dinner")[$groupIndex - 1];
    $menuEntry = new MenuEntry();
    $menuEntry->internalID = "$internalID";
    $menuEntry->foodName = "$groupName $groupVariantIndex";
    $menuEntry->foodGroup = "$groupName";
    $menuEntry->foodText = "$groupName text $groupVariantIndex on " . date("D Y-m-d ", $date);
    $menuEntry->price = $groupIndex * 10 + $groupVariantIndex;
    if ($user > 2)
        $menuEntry->price *= 2;
    $menuEntry->date = $date * 1000;
    $menuEntry->labels = "";

    switch ($groupIndex) {
        case 1: // Breakfast
            $menuEntry->features = new SupportedFeatures();
            $menuEntry->features->orders = $today != $date;
            $menuEntry->features->foodStock = false;
            $menuEntry->features->remainingFood = false;
            break;
        case 2: // Lunch
            $menuEntry->features = new SupportedFeatures();
            $menuEntry->features->orders = $today != $date;
            $menuEntry->features->foodStock = $today == $date;
            $menuEntry->features->remainingFood = true;
            break;
        case 3: // Dinner
            $menuEntry->features = new SupportedFeatures();
            $menuEntry->features->orders = $today != $date;
            $menuEntry->features->foodStock = false;
            $menuEntry->features->remainingFood = false;
            break;
        default:
            die("Unknown food group name");
    }

    return $menuEntry;
}

date_default_timezone_set('UTC');
