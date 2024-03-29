package one.digitalinnovation.investment.service;

import one.digitalinnovation.investment.builder.InvestmentDTOBuilder;
import one.digitalinnovation.investment.dto.InvestmentDTO;
import one.digitalinnovation.investment.entity.Investment;
import one.digitalinnovation.investment.exception.InsufficientBalanceForWithdrawalException;
import one.digitalinnovation.investment.exception.InvestmentAlreadyRegisteredException;
import one.digitalinnovation.investment.exception.InvestmentInvalidExpirationDateException;
import one.digitalinnovation.investment.exception.InvestmentNotFoundException;
import one.digitalinnovation.investment.mapper.InvestmentMapper;
import one.digitalinnovation.investment.repository.InvestmentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentServiceTest {

    private static final long INVALID_INVESTMENT_ID = 1L;
    private final InvestmentMapper investmentMapper = InvestmentMapper.INSTANCE;

    @Mock
    private InvestmentRepository investmentRepository;

    @InjectMocks
    private InvestmentService investmentService;

    // creation
    @Test
    void whenInvestmentInformedThenItShouldBeCreated() throws InvestmentAlreadyRegisteredException, InvestmentInvalidExpirationDateException {
        // given
        InvestmentDTO expectedInvestmentDTO = InvestmentDTOBuilder.builder().build().toInvestmentDTO();
        Investment expectedSavedInvestment = investmentMapper.toModel(expectedInvestmentDTO);

        // when
        when(investmentRepository.findByName(expectedInvestmentDTO.getName())).thenReturn(Optional.empty());
        when(investmentRepository.save(expectedSavedInvestment)).thenReturn(expectedSavedInvestment);

        // then
        InvestmentDTO createdInvestmentDTO = investmentService.createInvestment(expectedInvestmentDTO);

        assertThat(createdInvestmentDTO.getId(), is(equalTo(expectedInvestmentDTO.getId())));
        assertThat(createdInvestmentDTO.getName(), is(equalTo(expectedInvestmentDTO.getName())));
        assertThat(createdInvestmentDTO.getValue(), is(equalTo(expectedInvestmentDTO.getValue())));
        assertThat(createdInvestmentDTO.getInitialDate(), is(lessThan(createdInvestmentDTO.getExpirationDate())));
    }

    @Test
    void whenAlreadyRegisteredInvestmentInformedThanAnExceptionShouldBeThrown() {
        // given
        InvestmentDTO expectedInvestmentDTO = InvestmentDTOBuilder.builder().build().toInvestmentDTO();
        Investment duplicatedInvestment = investmentMapper.toModel(expectedInvestmentDTO);

        // when
        when(investmentRepository.findByName(expectedInvestmentDTO.getName())).thenReturn(Optional.of(duplicatedInvestment));

        // then
        assertThrows(InvestmentAlreadyRegisteredException.class, () -> investmentService.createInvestment(expectedInvestmentDTO));
    }

    @Test
    void whenInvestmentWithInitialDateGreaterThanExpirationDateInformedThanExceptionShouldBeThrown() {
        // given
        InvestmentDTO expectedInvestmentDTO = InvestmentDTOBuilder.builder()
                .initialDate(LocalDate.of(2000, 1, 1))
                .expirationDate(LocalDate.of(1970, 1, 1))
                .build().toInvestmentDTO();

        // then
        assertThrows(InvestmentInvalidExpirationDateException.class, () -> investmentService.createInvestment(expectedInvestmentDTO));
    }

    // get investment
    @Test
    void whenValidInvestmentNameIsGivenThenReturnAnInvestment() throws InvestmentNotFoundException {
        // given
        InvestmentDTO expectedFoundInvestmentDTO = InvestmentDTOBuilder.builder().build().toInvestmentDTO();
        Investment expectedFoundInvestment = investmentMapper.toModel(expectedFoundInvestmentDTO);

        // when
        when(investmentRepository.findByName(expectedFoundInvestment.getName())).thenReturn(Optional.of(expectedFoundInvestment));

        // then
        InvestmentDTO foundInvestmentDTO = investmentService.findByName(expectedFoundInvestmentDTO.getName());

        assertThat(foundInvestmentDTO, is(equalTo(expectedFoundInvestmentDTO)));
    }

    @Test
    void whenNoRegisteredInvestmentNameIsGivenThenThrowAnException() {
        // given
        InvestmentDTO expectedFoundInvestmentDTO = InvestmentDTOBuilder.builder().build().toInvestmentDTO();

        // when
        when(investmentRepository.findByName(expectedFoundInvestmentDTO.getName())).thenReturn(Optional.empty());

        // then
        assertThrows(InvestmentNotFoundException.class, () -> investmentService.findByName(expectedFoundInvestmentDTO.getName()));
    }

    // list investments
    @Test
    void whenListInvestmentIsCalledThenReturnAListOfInvestments() {
        // given
        InvestmentDTO expectedInvestmentDTO = InvestmentDTOBuilder.builder().build().toInvestmentDTO();
        Investment expectedFoundInvestment = investmentMapper.toModel(expectedInvestmentDTO);

        // when
        when(investmentRepository.findAll()).thenReturn(Collections.singletonList(expectedFoundInvestment));

        // then
        List<InvestmentDTO> foundInvestmentDTO = investmentService.listAll();

        assertThat(foundInvestmentDTO, is(not(empty())));
        assertThat(foundInvestmentDTO.get(0), is(equalTo(expectedInvestmentDTO)));
    }

    @Test
    void whenListInvestmentIsCalledThenReturnAnEmptyListOfInvestments() {
        // when
        when(investmentRepository.findAll()).thenReturn(emptyList());

        // then
        List<InvestmentDTO> foundInvestmentDTO = investmentService.listAll();

        assertThat(foundInvestmentDTO, is(empty()));
    }

    // delete
    @Test
    void whenExclusionIsCalledWithValidIdThenAnInvestmentIsDeleted() throws InvestmentNotFoundException {
        // given
        InvestmentDTO expectedDeletedInvestmentDTO = InvestmentDTOBuilder.builder().build().toInvestmentDTO();
        Investment expectedDeletedInvestment = investmentMapper.toModel(expectedDeletedInvestmentDTO);

        // when
        when(investmentRepository.findById(expectedDeletedInvestmentDTO.getId())).thenReturn(Optional.of(expectedDeletedInvestment));
        doNothing().when(investmentRepository).deleteById(expectedDeletedInvestmentDTO.getId());

        // then
        investmentService.deleteById(expectedDeletedInvestmentDTO.getId());

        verify(investmentRepository, times(1)).findById(expectedDeletedInvestmentDTO.getId());
        verify(investmentRepository, times(1)).deleteById(expectedDeletedInvestmentDTO.getId());
    }

    // apply
    @Test
    void whenApplyIsCalledThenAmountShouldBeApplied() throws InvestmentNotFoundException {
        // given
        InvestmentDTO expectedAppliedInvestmentDTO = InvestmentDTOBuilder.builder().build().toInvestmentDTO();
        Investment expectedAppliedInvestment = investmentMapper.toModel(expectedAppliedInvestmentDTO);

        // when
        when(investmentRepository.findById(expectedAppliedInvestmentDTO.getId())).thenReturn(Optional.of(expectedAppliedInvestment));
        when(investmentRepository.save(expectedAppliedInvestment)).thenReturn(expectedAppliedInvestment);

        double applicationAmount = 1000;
        double expectedValueAfterApplication = expectedAppliedInvestmentDTO.getValue() + applicationAmount;

        // then
        InvestmentDTO appliedInvestmentDTO = investmentService.apply(expectedAppliedInvestmentDTO.getId(), applicationAmount);

        assertThat(appliedInvestmentDTO.getValue(), is(equalTo(expectedValueAfterApplication)));
    }

    @Test
    void whenApplyIsCalledWithInvalidInvestmentIdThenThrowException() {
        // when
        when(investmentRepository.findById(INVALID_INVESTMENT_ID)).thenReturn(Optional.empty());

        // then
        assertThrows(InvestmentNotFoundException.class, () -> investmentService.apply(INVALID_INVESTMENT_ID, 1000));
    }

    // withdraw
    @Test
    void whenWithdrawIsCalledThenAmountShouldBeWithdrawn() throws InvestmentNotFoundException, InsufficientBalanceForWithdrawalException {
        // given
        InvestmentDTO expectedWithdrawnInvestmentDTO = InvestmentDTOBuilder.builder().build().toInvestmentDTO();
        Investment expectedWithdrawInvestment = investmentMapper.toModel(expectedWithdrawnInvestmentDTO);

        // when
        when(investmentRepository.findById(expectedWithdrawnInvestmentDTO.getId())).thenReturn(Optional.of(expectedWithdrawInvestment));
        when(investmentRepository.save(expectedWithdrawInvestment)).thenReturn(expectedWithdrawInvestment);

        double withdrawalAmount = expectedWithdrawnInvestmentDTO.getValue() * .1;
        double expectedValueAfterWithdrawal = expectedWithdrawnInvestmentDTO.getValue() - withdrawalAmount;

        // then
        InvestmentDTO withdrawnInvestmentDTO = investmentService.withdraw(expectedWithdrawnInvestmentDTO.getId(), withdrawalAmount);

        assertThat(withdrawnInvestmentDTO.getValue(), is(equalTo(expectedValueAfterWithdrawal)));

    }

    @Test
    void whenWithdrawIsCalledWithInvalidInvestmentIdThenThrowException() {
        // when
        when(investmentRepository.findById(INVALID_INVESTMENT_ID)).thenReturn(Optional.empty());

        // then
        assertThrows(InvestmentNotFoundException.class, () -> investmentService.withdraw(INVALID_INVESTMENT_ID, 1000));
    }

    @Test
    void whenWithdrawAmountIsGreaterThanBalanceThenThrowException() {
        // given
        InvestmentDTO expectedWithdrawnInvestmentDTO = InvestmentDTOBuilder.builder().build().toInvestmentDTO();
        Investment expectedWithdrawInvestment = investmentMapper.toModel(expectedWithdrawnInvestmentDTO);

        // when
        when(investmentRepository.findById(expectedWithdrawnInvestmentDTO.getId())).thenReturn(Optional.of(expectedWithdrawInvestment));

        double withdrawalAmount = expectedWithdrawnInvestmentDTO.getValue() + 1;

        // then
        assertThrows(InsufficientBalanceForWithdrawalException.class, () -> investmentService.withdraw(expectedWithdrawnInvestmentDTO.getId(), withdrawalAmount));

    }
}