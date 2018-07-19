-- phpMyAdmin SQL Dump
-- version 4.4.4
-- http://www.phpmyadmin.net
--

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Databáze: `sfm`
--

-- --------------------------------------------------------

--
-- Struktura tabulky `FoodOrder`
--

CREATE TABLE IF NOT EXISTS `FoodOrder` (
  `FOID` int(11) NOT NULL,
  `FOUserID` int(11) NOT NULL,
  `FODate` int(11) NOT NULL,
  `FOFoodID` bigint(20) DEFAULT NULL,
  `FOType` text NOT NULL,
  `FOReserved` int(11) NOT NULL,
  `FOOffered` int(11) NOT NULL,
  `FOTaken` int(11) NOT NULL,
  `FODescription` text,
  `FOPrice` int(11) NOT NULL
) ENGINE=MyISAM AUTO_INCREMENT=124 DEFAULT CHARSET=utf8;

--
-- Vypisuji data pro tabulku `FoodOrder`
--

INSERT INTO `FoodOrder` (`FOID`, `FOUserID`, `FODate`, `FOFoodID`, `FOType`, `FOReserved`, `FOOffered`, `FOTaken`, `FODescription`, `FOPrice`) VALUES
(1, 2, 1500007883, 150007680011, 'standard', 1, 0, 0, NULL, 11),
(2, 1, 1505164790, 150517440031, 'standard', 1, 0, 0, NULL, 11),
(34, 1, 2147483647, 152988480011, 'standard', 1, 0, 0, NULL, 11),
(9, 2, 1529881675, 152988480012, 'standard', 2, 0, 0, NULL, 0),
(13, 1, 1517871600, NULL, 'payment', 1, 0, 1, 'Čokoláda na nervy', 20),
(14, 1, 1529618400, NULL, 'payment', 1, 0, 1, 'Čokoláda', 20),
(55, 1, 2147483647, 153005760022, 'standard', 1, 0, 0, NULL, 22),
(42, 1, 2147483647, 152997120023, 'standard', 1, 0, 0, NULL, 23),
(35, 1, 2147483647, 152997120012, 'standard', 1, 0, 0, NULL, 12),
(57, 1, 2147483647, 153040320013, 'standard', 1, 0, 0, NULL, 13),
(86, 1, 2147483647, 153057600011, 'standard', 1, 0, 0, NULL, 11),
(51, 1, 2147483647, 153040320032, 'standard', 1, 0, 0, NULL, 32),
(52, 1, 2147483647, 153005760013, 'standard', 1, 0, 0, NULL, 13),
(53, 1, 2147483647, 153014400012, 'standard', 4, 0, 0, NULL, 12),
(54, 1, 2147483647, 153014400031, 'standard', 1, 0, 0, NULL, 31),
(120, 1, 1530835238, 153092160023, 'standard', 1, 0, 0, NULL, 23),
(83, 1, 2147483647, 153048960031, 'standard', 1, 0, 0, NULL, 31),
(85, 1, 2147483647, 153040320023, 'standard', 1, 0, 0, NULL, 23),
(119, 1, 1530659320, 153057600021, 'standard', 1, 1, 0, NULL, 21),
(70, 1, 2147483647, 153057600031, 'standard', 1, 0, 0, NULL, 31),
(77, 1, 2147483647, 153118080012, 'standard', 1, 0, 0, NULL, 12),
(81, 1, 2147483647, 153048960013, 'standard', 1, 0, 0, NULL, 13),
(78, 1, 2147483647, 153118080031, 'standard', 1, 0, 0, NULL, 31),
(113, 1, 1530634897, 153074880012, 'standard', 1, 0, 0, NULL, 12),
(110, 1, 1530618730, 153074880022, 'standard', 3, 0, 0, NULL, 22),
(108, 1, 1530618729, 153074880031, 'standard', 1, 0, 0, NULL, 31),
(74, 1, 2147483647, 153100800012, 'standard', 1, 0, 0, NULL, 12),
(123, 1, 1530837620, 153100800021, 'standard', 1, 0, 0, NULL, 21),
(76, 1, 2147483647, 153100800033, 'standard', 1, 0, 0, NULL, 33),
(105, 1, 1530554335, 153048960023, 'standard', 2, 1, 1, NULL, 23),
(91, 1, 1531612800, NULL, 'payment', 1, 0, 1, 'Tousty', 15),
(118, 1, 1530647941, 153066240013, 'standard', 1, 0, 0, NULL, 13),
(121, 1, 1530835393, 153092160013, 'standard', 4, 0, 0, NULL, 13);

--
-- Klíče pro exportované tabulky
--

--
-- Klíče pro tabulku `FoodOrder`
--
ALTER TABLE `FoodOrder`
  ADD PRIMARY KEY (`FOID`);

--
-- AUTO_INCREMENT pro tabulky
--

--
-- AUTO_INCREMENT pro tabulku `FoodOrder`
--
ALTER TABLE `FoodOrder`
  MODIFY `FOID` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=124;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
