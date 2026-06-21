package com.tradelog.position;

import com.tradelog.trade.TradeLeg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    PositionRepository positionRepository;

    @InjectMocks
    PositionService positionService;

    private TradeLeg optionLeg(String action, int qty, double price) {
        TradeLeg leg = new TradeLeg();
        leg.setCampaignId(1L);
        leg.setInstrumentType("OPTION");
        leg.setAction(action);
        leg.setTicker("SPY");
        leg.setQuantity(qty);
        leg.setPrice(price);
        leg.setNetCashFlow(action.startsWith("S") ? qty * price * 100 : -(qty * price * 100));
        leg.setOptionType("PUT");
        leg.setStrike(480.0);
        leg.setExpiry(LocalDate.of(2026, 12, 20));
        leg.setTradedAt(LocalDate.now());
        return leg;
    }

    private TradeLeg stockLeg(String action, int qty, double price) {
        TradeLeg leg = new TradeLeg();
        leg.setCampaignId(1L);
        leg.setInstrumentType("STOCK");
        leg.setAction(action);
        leg.setTicker("NVDA");
        leg.setQuantity(qty);
        leg.setPrice(price);
        leg.setNetCashFlow(action.equals("STC") ? qty * price : -(qty * price));
        leg.setTradedAt(LocalDate.now());
        return leg;
    }

    @Test
    void openingSto_createsNewPosition() {
        when(positionRepository.findOpenOptionPosition(anyLong(), anyString(), anyString(), anyDouble(), any()))
                .thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        positionService.applyLeg(optionLeg("STO", 5, 2.35));

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position saved = captor.getValue();
        assertThat(saved.getOpenAction()).isEqualTo("STO");
        assertThat(saved.getOpenQuantity()).isEqualTo(5);
        assertThat(saved.getAvgPrice()).isEqualTo(2.35);
        assertThat(saved.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void secondSto_averagesPrice() {
        Position existing = new Position();
        existing.setOpenAction("STO");
        existing.setOpenQuantity(5);
        existing.setAvgPrice(2.00);
        existing.setStatus("OPEN");

        when(positionRepository.findOpenOptionPosition(anyLong(), anyString(), anyString(), anyDouble(), any()))
                .thenReturn(Optional.of(existing));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        positionService.applyLeg(optionLeg("STO", 5, 3.00));

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        assertThat(captor.getValue().getOpenQuantity()).isEqualTo(10);
        assertThat(captor.getValue().getAvgPrice()).isEqualTo(2.50);
    }

    @Test
    void btc_reducesQuantity() {
        Position existing = new Position();
        existing.setOpenQuantity(5);
        existing.setAvgPrice(2.35);
        existing.setStatus("OPEN");

        when(positionRepository.findOpenOptionPosition(anyLong(), anyString(), anyString(), anyDouble(), any()))
                .thenReturn(Optional.of(existing));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        positionService.applyLeg(optionLeg("BTC", 3, 1.00));

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        assertThat(captor.getValue().getOpenQuantity()).isEqualTo(2);
        assertThat(captor.getValue().getStatus()).isEqualTo("OPEN");
    }

    @Test
    void btc_toZero_closesPosition() {
        Position existing = new Position();
        existing.setOpenQuantity(5);
        existing.setAvgPrice(2.35);
        existing.setStatus("OPEN");

        when(positionRepository.findOpenOptionPosition(anyLong(), anyString(), anyString(), anyDouble(), any()))
                .thenReturn(Optional.of(existing));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        positionService.applyLeg(optionLeg("BTC", 5, 1.00));

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        assertThat(captor.getValue().getOpenQuantity()).isEqualTo(0);
        assertThat(captor.getValue().getStatus()).isEqualTo("CLOSED");
        assertThat(captor.getValue().getClosedAt()).isNotNull();
    }

    @Test
    void expired_closesPosition() {
        Position existing = new Position();
        existing.setOpenQuantity(5);
        existing.setStatus("OPEN");

        when(positionRepository.findOpenOptionPosition(anyLong(), anyString(), anyString(), anyDouble(), any()))
                .thenReturn(Optional.of(existing));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TradeLeg leg = optionLeg("EXPIRED", 5, 0.0);
        positionService.applyLeg(leg);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void btc_positionNotFound_throwsBadRequest() {
        when(positionRepository.findOpenOptionPosition(anyLong(), anyString(), anyString(), anyDouble(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> positionService.applyLeg(optionLeg("BTC", 5, 1.00)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No open position");
    }

    @Test
    void openingBto_stock_createsNewPosition() {
        when(positionRepository.findOpenStockPosition(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        positionService.applyLeg(stockLeg("BTO", 100, 820.0));

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        assertThat(captor.getValue().getOpenQuantity()).isEqualTo(100);
        assertThat(captor.getValue().getAvgPrice()).isEqualTo(820.0);
    }

    @Test
    void assigned_createsPositionWithBtoAction() {
        when(positionRepository.findOpenStockPosition(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TradeLeg leg = stockLeg("ASSIGNED", 100, 480.0);
        positionService.applyLeg(leg);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        assertThat(captor.getValue().getOpenAction()).isEqualTo("BTO");
        assertThat(captor.getValue().getOpenQuantity()).isEqualTo(100);
        assertThat(captor.getValue().getAvgPrice()).isEqualTo(480.0);
    }
}
