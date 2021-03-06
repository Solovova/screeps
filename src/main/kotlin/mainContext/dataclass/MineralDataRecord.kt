package mainContext.dataclass

class MineralDataRecord(var quantity: Int = 0,
                        var quantityUp: Int = 0,
                        var quantityDown: Int = 0,
                        var need: Int = 0,
                        var onlyDirectBuy: Boolean = false, //direct buy then quantity < storeMax && realPrice < priceMin
                        //Market buy
                        var priceMax: Double = 0.0,
                        var marketBuyLack: Int = 0,
                        var buyToRoom: String = "",
                        //Market sell
                        var priceMin: Double = 0.0,
                        var marketSellExcess: Int = 100000000, //if 0 - sell always
                        var sellFromRoom: String = "",
                        //Production and harvesting
                        var storeMax: Int = 0,  //production control if stored>storeMax stop produce and harvest
                        var balancingStart: Int = 50000, //Use in reactionActiveArr
                        var balancingStop: Int = 100000,

                        var avgBuyPrice: Double = 0.0,
                        var avgSellPrice: Double = 0.0

)

//Sell algorithm
//first direct sell, if can sell for real price > (priceMax + priceMin)/2 - sell
//create sell order from room sellFromRoom, price calculate as all sell orders but not min price,
//берем сортированые ордера от меньшей цены к большей, и отсчитываем 20000 елемента, чтоб не ставить минимальную цену
//пример
//O 5000 0.39
//O 9000 0.41
//O 40000 0.42
//первые 2 пропускаем 5000+9000<20000
//ставим order по 0.42 5000+9000+40000>20000