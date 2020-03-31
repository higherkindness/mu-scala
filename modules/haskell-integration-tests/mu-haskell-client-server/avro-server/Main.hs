{-# language DataKinds             #-}
{-# language FlexibleContexts      #-}
{-# language PartialTypeSignatures #-}
{-# language OverloadedLabels      #-}
{-# language OverloadedStrings     #-}
{-# language TypeApplications      #-}
{-# OPTIONS_GHC -fno-warn-partial-type-signatures #-}

module Main where

import Data.Functor.Identity
import Data.Maybe (fromMaybe)
import Data.Text as T (Text)
import Mu.GRpc.Server
import Mu.Server
import Mu.Schema.Optics

import AvroProtocol

main :: IO ()
main = runGRpcApp msgAvro 9124 server

ping :: (MonadServer m) => m ()
ping = return ()

getForecast :: (MonadServer m) => GetForecastRequest -> m GetForecastResponse
getForecast req =
  return resp
    where
      days      = fromIntegral (req ^. #days_required)
      forecasts = sunnyDays days
      resp      = record (lastUpdated, forecasts)

lastUpdated :: T.Text
lastUpdated = "2020-03-20T12:00:00Z"

sunnyDays :: Int -> [Weather]
sunnyDays n = replicate n (enum @"SUNNY")

server :: (MonadServer m) => ServerT Identity WeatherService m _
server = Server (getForecast :<|>: ping :<|>: H0)
