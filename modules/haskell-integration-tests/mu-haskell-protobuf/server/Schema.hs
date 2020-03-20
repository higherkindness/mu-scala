{-# language DataKinds             #-}
{-# language DeriveAnyClass        #-}
{-# language DeriveGeneric         #-}
{-# language DuplicateRecordFields #-}
{-# language FlexibleContexts      #-}
{-# language FlexibleInstances     #-}
{-# language MultiParamTypeClasses #-}
{-# language PolyKinds             #-}
{-# language TemplateHaskell       #-}
{-# language TypeFamilies          #-}
{-# language TypeOperators         #-}

module Schema where

import Data.Text as T
import GHC.Generics

import Mu.Quasi.GRpc
import Mu.Schema

grpc "WeatherProtocol" id "weather.proto"

type GetForecastRequest = Term Maybe WeatherProtocol (WeatherProtocol :/: "GetForecastRequest")

type Weather = Term Maybe WeatherProtocol (WeatherProtocol :/: "Weather")

type GetForecastResponse = Term Maybe WeatherProtocol (WeatherProtocol :/: "GetForecastResponse")
