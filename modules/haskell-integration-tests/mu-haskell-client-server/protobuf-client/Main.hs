{-# language DataKinds             #-}
{-# language FlexibleContexts      #-}
{-# language PartialTypeSignatures #-}
{-# language OverloadedLabels      #-}
{-# language OverloadedStrings     #-}
{-# language TypeApplications      #-}
{-# OPTIONS_GHC -fno-warn-partial-type-signatures #-}

module Main where

import Data.Maybe (fromMaybe)
import Data.List (intercalate)
import Data.Text as T (Text, pack, unpack)
import System.Environment (getArgs)
import Text.Read (readMaybe)

import Mu.GRpc.Client.Optics
import Mu.Schema.Optics

import Protocol

main :: IO ()
main = do
  args <- getArgs
  let config = grpcClientConfigSimple (head args) 9123 False
  Right conn <- initGRpc config msgProtoBuf @WeatherService
  case (tail args) of
    ["ping"] -> ping conn
    ["get-forecast", city, days] -> getForecast conn city days
    _ -> putStrLn "Invalid args"

ping :: GRpcConnection WeatherService 'MsgProtoBuf -> IO ()
ping client = do
  client ^. #ping
  putStrLn "pong"

getForecast :: GRpcConnection WeatherService 'MsgProtoBuf -> String -> String -> IO ()
getForecast client city days = do
  GRpcOk resp <- (client ^. #getForecast) req
  putStrLn $ showGetForecastResponse resp
    where
      c = Just(T.pack city)
      d = readMaybe days
      req = record (c, d)

showGetForecastResponse :: GetForecastResponse -> String
showGetForecastResponse resp =
  lastUpdated ++ " " ++ (intercalate ", " dailyForecasts)
    where
      lastUpdated = unpack(fromMaybe "" (resp ^. #last_updated))
      -- TODO actually I want to print just "SUNNY" here, but it's too hard
      dailyForecasts = show <$> (fromMaybe [] (resp ^. #daily_forecasts))
