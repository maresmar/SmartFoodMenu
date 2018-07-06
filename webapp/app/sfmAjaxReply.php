<?php

/**
 * Provides proxy attributes around real (important) data to handle API updates
 */
class SfmAjaxReply
{
    public $apiVersion = 1;
    public $replyType = "unknown";
    public $updateMsg = null;
    public $data;
}

?>
