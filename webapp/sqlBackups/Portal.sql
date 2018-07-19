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
-- Struktura tabulky `Portal`
--

CREATE TABLE IF NOT EXISTS `Portal` (
  `PID` int(11) NOT NULL,
  `PGID` int(11) DEFAULT NULL,
  `PName` varchar(90) NOT NULL,
  `PPlugin` text NOT NULL,
  `PRef` text NOT NULL,
  `PLocN` double NOT NULL,
  `PLocE` double NOT NULL,
  `PExtra` text,
  `PSecurity` int(11) NOT NULL,
  `PFeatures` int(11) NOT NULL
) ENGINE=MyISAM AUTO_INCREMENT=19 DEFAULT CHARSET=utf8;

--
-- Vypisuji data pro tabulku `Portal`
--

INSERT INTO `Portal` (`PID`, `PGID`, `PName`, `PPlugin`, `PRef`, `PLocN`, `PLocE`, `PExtra`, `PSecurity`, `PFeatures`) VALUES
(1, 1, 'Jihlava, Na Stoupách', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'http://195.113.207.81:8080/nastoupach', 49.400301, 15.59136, '{"portalVersion":"2.10","portalAutoUpdate":"✔️","allergensPattern":""}', 102, 32),
(2, 2, 'Jihlava, Karoliny Světlé', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'http://skola.ozs-ji.cz:8080/karolina', 49.40135, 15.579092, '{"portalVersion":"2.10","portalAutoUpdate":"✔️","allergensPattern":""}', 102, 32),
(3, 3, 'Jihlava, Menza VŠPJ', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://icanteen.vspj.cz', 49.398821, 15.582756, '{"portalVersion":"2.10","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(4, 4, 'Jihlava, ZŠ Seifertova', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://strav.nasejidelna.cz/0004', 49.393731, 15.584128, '{"portalVersion":"2.14","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(5, 5, 'Brno, SPŠEIT Brno', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://zion.sspbrno.cz', 49.225631, 16.580791, '{"portalVersion":"2.13","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(6, 6, 'Ostrava, SPSEI Ostrava', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'http://obedy.spseiostrava.cz:8080', 49.839325, 18.29376, '{"portalVersion":"2.13","portalAutoUpdate":"✔️","allergensPattern":""}', 102, 32),
(7, 7, 'Plzeň, SPSE Plzeň', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://strava.spseplzen.cz', 49.732717, 13.404119, '{"portalVersion":"2.13","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(8, 8, 'Benešov nad Ploučnicí, Školní jídelna Benešov nad Ploučnicí', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'http://109.81.195.25', 50.743222, 14.305566, '{"portalVersion":"2.13","portalAutoUpdate":"✔️","allergensPattern":""}', 102, 32),
(9, 9, 'Havlíčkův Brod, ZŠ Štáflova', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://strav.nasejidelna.cz/0060', 49.609431, 15.5801, '{"portalVersion":"2.14","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(10, 10, 'Olomouc,  SZŠ a VOŠz E. P.', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'http://jidelna.epol.cz:8090', 49.59639, 17.245234, '{"portalVersion":"2.10","portalAutoUpdate":"✔️","allergensPattern":""}', 102, 32),
(11, 11, 'Ceska Lipa, ZS Slovanka', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'http://jidelna.zsslovanka.cz', 50.69206, 14.527378, '{"portalVersion":"2.13","portalAutoUpdate":"✔️","allergensPattern":""}', 102, 32),
(12, 12, 'Havířov-Šumbark, SŠTO Lidická', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://strav.nasejidelna.cz/0112', 49.797278, 18.409726, '{"portalVersion":"2.14","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(13, 13, 'Zlín, Gymnázium Zlín - Lesní čtvrť', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://strav.nasejidelna.cz/0077', 49.218195, 17.692335, '{"portalVersion":"2.14","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(14, 14, 'Chomutov, ZŠ Chomutov', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://strav.nasejidelna.cz/0212', 50.488362, 13.441641, '{"portalVersion":"2.14","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(15, 15, 'Havířov-Podlesí, ZŠ Fr. Hrubína', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://strav.nasejidelna.cz/0110', 49.776145, 18.460999, '{"portalVersion":"2.14","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(16, 16, 'Kolín, Zš Kolín II.', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://strav.nasejidelna.cz/0134', 50.02736, 15.19515, '{"portalVersion":"2.14","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32),
(17, 17, 'Vítkov, Základní škola a gymnázium Vítkov', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'http://85.163.4.0:9999', 49.777601, 17.757487, '{"portalVersion":"2.13","portalAutoUpdate":"✔️","allergensPattern":""}', 102, 32),
(18, 18, 'Ostrava, Jazykové gymnázium Pavla Tigrida', 'cz.maresmar.sfm/cz.maresmar.sfm.builtin.ICanteenService', 'https://mail.jazgym.cz/stravovani', 49.824569, 18.171726, '{"portalVersion":"2.13","portalAutoUpdate":"✔️","allergensPattern":""}', 100, 32);

--
-- Klíče pro exportované tabulky
--

--
-- Klíče pro tabulku `Portal`
--
ALTER TABLE `Portal`
  ADD PRIMARY KEY (`PID`),
  ADD UNIQUE KEY `PName` (`PName`);

--
-- AUTO_INCREMENT pro tabulky
--

--
-- AUTO_INCREMENT pro tabulku `Portal`
--
ALTER TABLE `Portal`
  MODIFY `PID` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=19;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
